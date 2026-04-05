package com.windowvanish.client;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.windowVanish2.client.ExtendedUser32;
import com.windowvanish.WindowVanish;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class VanishManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowVanish.MOD_ID);

    private static WinUser.HHOOK hHook;

    private static final AtomicBoolean isHidden = new AtomicBoolean(false);
    private static long windowHandle = -1;


    public static void forceFocus(WinDef.HWND targetHwnd) {
        ExtendedUser32 u32 = ExtendedUser32.INSTANCE;

        int currentThreadID = Kernel32.INSTANCE.GetCurrentThreadId();
        int foregroundThreadID = u32.GetWindowThreadProcessId(u32.GetForegroundWindow(), null);

        if (foregroundThreadID != currentThreadID) {
            u32.AttachThreadInput(new WinDef.DWORD(currentThreadID), new WinDef.DWORD(foregroundThreadID), true);

            u32.keybd_event((byte) 0x12, (byte) 0, 0, new BaseTSD.ULONG_PTR(0));

            u32.SetForegroundWindow(targetHwnd);
            u32.SetFocus(targetHwnd);

            u32.keybd_event((byte) 0x12, (byte) 0, 0x0002, new BaseTSD.ULONG_PTR(0));

            u32.AttachThreadInput(new WinDef.DWORD(currentThreadID), new WinDef.DWORD(foregroundThreadID), false);
        } else {
            u32.SetForegroundWindow(targetHwnd);
        }

        u32.SetWindowPos(targetHwnd, new WinDef.HWND(new Pointer(-1)), 0, 0, 0, 0, 0x0040 | 0x0002 | 0x0001);
        u32.SetWindowPos(targetHwnd, new WinDef.HWND(new Pointer(-2)), 0, 0, 0, 0, 0x0040 | 0x0002 | 0x0001);
    }

    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_APPWINDOW = 0x00040000;

    private static void forceHide() {
        WinDef.HWND hwnd = new WinDef.HWND(new Pointer(windowHandle));
        ExtendedUser32 user32 = ExtendedUser32.INSTANCE;

        int style = user32.GetWindowLong(hwnd, GWL_EXSTYLE);
        user32.SetWindowLong(hwnd, GWL_EXSTYLE, (style | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW);

        user32.SetWindowPos(hwnd, new WinDef.HWND(new Pointer(1)), 0, 0, 0, 0,
                0x0080 | 0x0010 | 0x0002 | 0x0001 | 0x0040); // HIDE | NOACTIVATE | NOMOVE | NOSIZE | SHOWWINDOW

        user32.ShowWindow(hwnd, WinUser.SW_MINIMIZE);

        User32.INSTANCE.SetForegroundWindow(User32.INSTANCE.GetDesktopWindow());

        CompletableFuture.runAsync(() -> {
            if (Minecraft.getInstance().isRunning()) {
                GLFW.glfwHideWindow(windowHandle);
                Minecraft.getInstance().getSoundManager().pauseAllExcept();
            }
        });
    }

    private static void show() {
        WinDef.HWND hwnd = new WinDef.HWND(new Pointer(windowHandle));
        ExtendedUser32 user32 = ExtendedUser32.INSTANCE;

        int style = user32.GetWindowLong(hwnd, GWL_EXSTYLE);
        user32.SetWindowLong(hwnd, GWL_EXSTYLE, (style & ~WS_EX_TOOLWINDOW) | WS_EX_APPWINDOW);

        // SW_SHOW (5) or SW_RESTORE (9).
        user32.ShowWindow(hwnd, 5);

        forceFocus(hwnd);

        user32.SwitchToThisWindow(hwnd, true);

        CompletableFuture.runAsync(() -> {
            if (Minecraft.getInstance().isRunning()) {
                GLFW.glfwShowWindow(windowHandle);
                // Ensure GLFW internal state matches
                GLFW.glfwFocusWindow(windowHandle);
                Minecraft.getInstance().getSoundManager().resume();
            }
        });
    }

    public static void toggleVisibility() {
        LOGGER.info("Toggling visibility");

        if (windowHandle == -1) {
            windowHandle = Minecraft.getInstance().getWindow().handle();
            LOGGER.info("Window handle: {}", windowHandle);
        }

        if (!isHidden.get()) {
            forceHide();
            isHidden.set(true);

            Thread musicStopper = getMusicStopper();
            musicStopper.start();
        } else {
            show();
            isHidden.set(false);


        }
    }

    private static @NotNull Thread getMusicStopper() {
        Thread musicStopper = new Thread(() -> {
            try {
                LOGGER.info("Starting pausing all sounds");
                while (isHidden.get()) {
                    Thread.sleep(1);
                    Minecraft.getInstance().getSoundManager().pauseAllExcept();
                }
                Minecraft.getInstance().getSoundManager().resume();
            } catch (Exception ignored) {}
        });
        musicStopper.setName("VanishMusicStopper");
        musicStopper.setDaemon(true);
        return musicStopper;
    }

    public static void registerGlobalShortcut() {
        if (!OperatingSytemChecker.isWindows()) {
            LOGGER.error("Cannot initialize WindowVanish on non-Windows machine");
            return;
        }

        Thread newThread = new Thread(() -> {
            User32 user32 = User32.INSTANCE;

            WinUser.LowLevelKeyboardProc keyboardHook = (nCode, wParam, info) -> {
                if (nCode >= 0) {
                    int vk = info.vkCode;
                    int message = wParam.intValue();

                    // 0x0100 = WM_KEYDOWN, 0x0104 = WM_SYSKEYDOWN (triggered when Alt is involved)
                    if (message == 0x0100 || message == 0x0104) {

                        // (info.flags & 0x20) checks the LLKHF_ALTDOWN bit.
                        boolean altIsDown = (info.flags & 0x20) != 0;
                        if (vk == 0x14 && altIsDown) {
                            toggleVisibility();
                            return new WinDef.LRESULT(1); // Swallow the keypress
                        }

                        // GetAsyncKeyState (0x8000) to check the physical state.
                        if (vk == 0x12 || vk == 0x11 || vk == 0xA4 || vk == 0xA5) { // VK_MENU / LALT / RALT
                            short capsLockState = user32.GetAsyncKeyState(0x14);
                            boolean capsIsDown = (capsLockState & 0x8000) != 0;

                            if (capsIsDown) {
                                toggleVisibility();
                                return new WinDef.LRESULT(1); // Swallow the keypress
                            }
                        }
                    }
                }

                return user32.CallNextHookEx(null, nCode, wParam, new WinDef.LPARAM(Pointer.nativeValue(info.getPointer())));
            };

            hHook = user32.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, null, 0);

            WinUser.MSG msg = new WinUser.MSG();
            LOGGER.info("Starting event listener");
            while (user32.GetMessage(msg, null, 0, 0) != 0) {
                user32.TranslateMessage(msg);
                user32.DispatchMessage(msg);
            }

            user32.UnhookWindowsHookEx(hHook);
        });

        newThread.setName("VanishListener");
        newThread.setDaemon(true);
        newThread.start();
    }
}
