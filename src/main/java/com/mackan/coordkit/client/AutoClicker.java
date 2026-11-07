package com.mackan.coordkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side auto-click and hold utilities for AFK use cases (e.g. mob farms,
 * concrete makers). All state is transient and never saved to config.
 *
 * <p>This uses a small amount of reflection to invoke private Minecraft methods
 * ({@code startAttack}, {@code continueAttack}, {@code startUseItem}) because
 * the mod has no mixins and these actions must be triggered reliably regardless
 * of physical mouse state. The mod targets Minecraft 26.2 specifically.</p>
 */
public final class AutoClicker {
	private static boolean autoClickEnabled;
	private static boolean holdAttackEnabled;
	private static boolean holdUseEnabled;
	private static int autoClickCooldown;

	private static Method startAttack;
	private static Method continueAttack;
	private static Method startUseItem;
	private static boolean reflectionReady;

	private AutoClicker() {
	}

	public static void toggleAutoClick(Minecraft client) {
		autoClickEnabled = !autoClickEnabled;
		holdAttackEnabled = false; // mutually exclusive with hold attack for clarity
		autoClickCooldown = 0;
		ClientFeedback.actionBar(client, "Auto-clicker " + (autoClickEnabled ? "ON" : "OFF"));
	}

	public static void toggleHoldAttack(Minecraft client) {
		holdAttackEnabled = !holdAttackEnabled;
		autoClickEnabled = false;
		ClientFeedback.actionBar(client, "Hold attack " + (holdAttackEnabled ? "ON" : "OFF"));
	}

	public static void toggleHoldUse(Minecraft client) {
		holdUseEnabled = !holdUseEnabled;
		ClientFeedback.actionBar(client, "Hold use " + (holdUseEnabled ? "ON" : "OFF"));
	}

	/**
	 * Called at the start of each client tick, before the game processes input,
	 * so held key states are visible to the game's attack/use handling.
	 */
	public static void startTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			resetIfWorldGone();
			return;
		}

		if (client.gui.screen() != null) {
			// Don't interfere while a screen is open
			return;
		}

		ensureReflection();

		if (holdAttackEnabled) {
			client.options.keyAttack.setDown(true);
			invokeContinueAttack(client);
		}

		if (holdUseEnabled) {
			client.options.keyUse.setDown(true);
		}
	}

	/**
	 * Called at the end of each client tick. Handles the periodic auto-click.
	 */
	public static void endTick(Minecraft client) {
		if (!autoClickEnabled) {
			return;
		}

		if (client.player == null || client.level == null) {
			autoClickEnabled = false;
			return;
		}

		if (client.gui.screen() != null) {
			return;
		}

		ensureReflection();

		if (autoClickCooldown > 0) {
			autoClickCooldown--;
			return;
		}

		invokeStartAttack(client);
		autoClickCooldown = Math.max(1, CoordKitConfig.get().autoClickDelayTicks);
	}

	public static boolean isAutoClickEnabled() {
		return autoClickEnabled;
	}

	public static boolean isHoldAttackEnabled() {
		return holdAttackEnabled;
	}

	public static boolean isHoldUseEnabled() {
		return holdUseEnabled;
	}

	public static boolean isAnyActive() {
		return autoClickEnabled || holdAttackEnabled || holdUseEnabled;
	}

	static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		if (!isAnyActive() || client.player == null) {
			return;
		}

		List<String> lines = new ArrayList<>(3);
		if (autoClickEnabled) {
			lines.add("Auto-click: ON (" + Math.max(1, CoordKitConfig.get().autoClickDelayTicks) + "t)");
		}
		if (holdAttackEnabled) {
			lines.add("Hold attack: ON");
		}
		if (holdUseEnabled) {
			lines.add("Hold use: ON");
		}

		if (lines.isEmpty()) {
			return;
		}

		int padding = 3;
		int lineHeight = 10;
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, client.font.width(line));
		}

		int x = padding;
		int y = graphics.guiHeight() - padding - lines.size() * lineHeight;

		graphics.fill(x - padding, y - padding, x + width + padding, y + lines.size() * lineHeight + padding - 1, 0x90000000);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(client.font, lines.get(i), x, y + i * lineHeight, 0xFFFFAA00, true);
		}
	}

	private static void resetIfWorldGone() {
		// keep toggles? better to turn off when leaving world to avoid surprises
		autoClickEnabled = false;
		holdAttackEnabled = false;
		holdUseEnabled = false;
		autoClickCooldown = 0;
	}

	private static void ensureReflection() {
		if (reflectionReady) {
			return;
		}
		try {
			startAttack = Minecraft.class.getDeclaredMethod("startAttack");
			startAttack.setAccessible(true);

			continueAttack = Minecraft.class.getDeclaredMethod("continueAttack", boolean.class);
			continueAttack.setAccessible(true);

			startUseItem = Minecraft.class.getDeclaredMethod("startUseItem");
			startUseItem.setAccessible(true);

			reflectionReady = true;
		} catch (Exception e) {
			reflectionReady = true; // don't retry every tick
			com.mackan.coordkit.CoordKit.LOGGER.warn("AutoClicker: failed to access Minecraft input methods", e);
		}
	}

	private static void invokeStartAttack(Minecraft client) {
		if (startAttack == null) return;
		try {
			startAttack.invoke(client);
		} catch (Exception e) {
			// ignore per-tick failures
		}
	}

	private static void invokeContinueAttack(Minecraft client) {
		if (continueAttack == null) return;
		try {
			continueAttack.invoke(client, client.gui.screen() == null);
		} catch (Exception e) {
			// ignore
		}
	}

	private static void invokeStartUseItem(Minecraft client) {
		if (startUseItem == null) return;
		try {
			startUseItem.invoke(client);
		} catch (Exception e) {
			// ignore
		}
	}
}
