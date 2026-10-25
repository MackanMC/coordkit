package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.CoordKitConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Settings with two tabs: HUD contents and waypoint display.
 */
public final class CoordKitConfigScreen extends Screen {
	private static final int TAB_HUD = 0;
	private static final int TAB_WAYPOINTS = 1;
	private static final int COLUMN_WIDTH = 150;
	private static final int COLUMN_GAP = 8;
	private static final int[] MARKER_RANGES = {64, 128, 256, 512, 1024, 2048, 4096};

	private final Screen parent;
	private final int tab;

	public CoordKitConfigScreen(Screen parent) {
		this(parent, TAB_HUD);
	}

	public CoordKitConfigScreen(Screen parent, int tab) {
		super(Component.literal("CoordKit Settings"));
		this.parent = parent;
		this.tab = tab;
	}

	@Override
	protected void init() {
		super.init();

		CoordKitConfig config = CoordKitConfig.get();
		int tabX = Math.max(4, this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2);

		Button hudTab = Button.builder(
				tabLabel("HUD", TAB_HUD),
				button -> switchTab(TAB_HUD)
		).bounds(tabX, 34, COLUMN_WIDTH, 20).build();
		hudTab.active = this.tab != TAB_HUD;
		addRenderableWidget(hudTab);

		Button waypointTab = Button.builder(
				tabLabel("Waypoints", TAB_WAYPOINTS),
				button -> switchTab(TAB_WAYPOINTS)
		).bounds(tabX + COLUMN_WIDTH + COLUMN_GAP, 34, COLUMN_WIDTH, 20).build();
		waypointTab.active = this.tab != TAB_WAYPOINTS;
		addRenderableWidget(waypointTab);

		if (this.tab == TAB_WAYPOINTS) {
			initWaypointTab(config);
		} else {
			initHudTab(config);
		}

		int width = COLUMN_WIDTH * 2 + COLUMN_GAP;
		int left = Math.max(4, (this.width - width) / 2);
		int bottom = this.height - 28;

		addRenderableWidget(Button.builder(
				Component.literal("Manage waypoints..."),
				button -> this.minecraft.gui.setScreen(new WaypointManagerScreen(this))
		).bounds(left, bottom, COLUMN_WIDTH, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Done"),
				button -> this.onClose()
		).bounds(left + COLUMN_WIDTH + COLUMN_GAP, bottom, COLUMN_WIDTH, 20).build());
	}

	private void initHudTab(CoordKitConfig config) {
		int left = Math.max(4, this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2);
		int right = left + COLUMN_WIDTH + COLUMN_GAP;
		int y = 64;

		addRenderableWidget(toggle(left, y, "HUD panel", () -> config.showHud, value -> config.showHud = value));
		addRenderableWidget(toggle(right, y, "Light level", () -> config.showLight, value -> config.showLight = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "FPS counter", () -> config.showFps, value -> config.showFps = value));
		addRenderableWidget(toggle(right, y, "Speed", () -> config.showSpeed, value -> config.showSpeed = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "Clock", () -> config.showClock, value -> config.showClock = value));
		addRenderableWidget(toggle(right, y, "Held durability", () -> config.showDurability, value -> config.showDurability = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "Chunk position", () -> config.showChunk, value -> config.showChunk = value));
		addRenderableWidget(toggle(right, y, "Waypoint list", () -> config.showWaypoints, value -> config.showWaypoints = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "Facing", () -> config.showFacing, value -> config.showFacing = value));
		addRenderableWidget(toggle(right, y, "Markers in view", () -> config.showMarkers, value -> config.showMarkers = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "Biome", () -> config.showBiome, value -> config.showBiome = value));
		addRenderableWidget(toggle(right, y, "Edge arrows", () -> config.markerOffscreenArrows, value -> config.markerOffscreenArrows = value));
		y += 20;

		addRenderableWidget(toggle(left, y, "Game time + weather", () -> config.showGameTime, value -> config.showGameTime = value));
	}

	private void initWaypointTab(CoordKitConfig config) {
		int left = Math.max(4, this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2);
		int width = COLUMN_WIDTH * 2 + COLUMN_GAP;
		int y = 68;

		addRenderableWidget(Button.builder(
				Component.literal("Max on HUD: " + config.maxWaypoints),
				button -> {
					config.maxWaypoints = config.maxWaypoints >= 10 ? 1 : config.maxWaypoints + 1;
					button.setMessage(Component.literal("Max on HUD: " + config.maxWaypoints));
					config.save();
				}
		).bounds(left, y, COLUMN_WIDTH, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Marker range: " + config.markerMaxDistance + "m"),
				button -> {
					config.markerMaxDistance = nextMarkerRange(config.markerMaxDistance);
					button.setMessage(Component.literal("Marker range: " + config.markerMaxDistance + "m"));
					config.save();
				}
		).bounds(left + COLUMN_WIDTH + COLUMN_GAP, y, COLUMN_WIDTH, 20).build());
		y += 26;

		addRenderableWidget(toggle(left, y, width, "Waypoint list", () -> config.showWaypoints, value -> config.showWaypoints = value));
		y += 24;

		addRenderableWidget(toggle(left, y, width, "Markers in view", () -> config.showMarkers, value -> config.showMarkers = value));
		y += 24;

		addRenderableWidget(toggle(left, y, width, "Edge arrows", () -> config.markerOffscreenArrows, value -> config.markerOffscreenArrows = value));
	}

	private void switchTab(int target) {
		if (target != this.tab) {
			this.minecraft.gui.setScreen(new CoordKitConfigScreen(this.parent, target));
		}
	}

	private Button toggle(int x, int y, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return toggle(x, y, COLUMN_WIDTH, label, getter, setter);
	}

	private Button toggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return Button.builder(label(label, getter.getAsBoolean()), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(label(label, getter.getAsBoolean()));
			CoordKitConfig.get().save();
		}).bounds(x, y, width, 20).build();
	}

	private Component tabLabel(String name, int tab) {
		return Component.literal(this.tab == tab ? "[ " + name + " ]" : name);
	}

	private static Component label(String label, boolean enabled) {
		return Component.literal(label + ": " + (enabled ? "ON" : "OFF"));
	}

	private static int nextMarkerRange(int current) {
		for (int i = 0; i < MARKER_RANGES.length; i++) {
			if (MARKER_RANGES[i] > current) {
				return MARKER_RANGES[i];
			}
		}

		return MARKER_RANGES[0];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("Changes are saved automatically"),
				this.width / 2, 21, 0xFFA0A0A0);
	}

	@Override
	public void onClose() {
		if (this.parent != null) {
			this.minecraft.gui.setScreen(this.parent);
		} else {
			super.onClose();
		}
	}
}
