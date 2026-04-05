package com.windowvanish.client.mixin;

import com.windowvanish.WindowVanish;
import com.windowvanish.client.OperatingSytemChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowVanish.MOD_ID);

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onPlayerConstructed(CallbackInfo ci) {
        if (!OperatingSytemChecker.isWindows()) {
            LOGGER.info("Queueing incompatible chat message on another thread");
            new Thread(() -> {
                try {
                    Thread.sleep(5_000);
                    if (!OperatingSytemChecker.isWindows()) {
                        Minecraft.getInstance().execute(() -> {
                            assert Minecraft.getInstance().player != null;
                            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("text.windowvanish.incompatibleOperatingSystem"));
                        });
                    }
                } catch (Throwable t) {
                    LOGGER.error("Chat message send failed: ", t);
                }
            }).start();
        }



    }
}
