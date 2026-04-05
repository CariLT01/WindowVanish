package com.windowvanish.client;

import com.windowvanish.client.VanishManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;

public class VanishClient implements ClientModInitializer {
	public static KeyMapping vanishKey;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		VanishManager.registerGlobalShortcut();
	}
}