package com.mackan.coordkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Small helpers for action-bar and chat feedback.
 */
public final class ClientFeedback {
	private ClientFeedback() {
	}

	public static void actionBar(Minecraft client, String message) {
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.literal(message));
		}
	}

	public static void chat(Minecraft client, String message) {
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(message));
		}
	}

	public static void chat(Minecraft client, Component message) {
		if (client.player != null) {
			client.player.sendSystemMessage(message);
		}
	}
}
