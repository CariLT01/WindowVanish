package com.windowvanish.client;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;

public interface ExtendedUser32 extends User32 {
    ExtendedUser32 INSTANCE = Native.load("user32", ExtendedUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean ShowWindowAsync(HWND hWnd, int nCmdShow);
    HWND SetActiveWindow(HWND hWnd);

    boolean LockSetForegroundWindow(int uLockCode);
    boolean AllowSetForegroundWindow(int dwProcessId);
    int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);
    int GetWindowLong(HWND hWnd, int nIndex);
    boolean BringWindowToTop(HWND hWnd);
    void SwitchToThisWindow(HWND hWnd, boolean fAltTab);

    void keybd_event(byte bVk, byte bScan, int dwFlags, ULONG_PTR dwExtraInfo);
    short GetKeyState(int nVirtKey);
}
