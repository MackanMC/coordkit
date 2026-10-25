package com.mackan.coordkit.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Draws the info panel, the nearby-waypoint list and the in-view markers.
 */
public final class CoordKitHud {
	private static final int PADDING = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int BACKDROP_COLOR = 0x90000000;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private CoordKitHud() {
	}

	static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		CoordKitConfig config = CoordKitConfig.get();

		if (client.player == null || client.level == null) {
			return;
		}

		// Markers work on their own, even with the info panel turned off.
		if (config.showMarkers) {
			WaypointMarkers.render(graphics, client, config);
		}

		if (!config.showHud) {
			return;
		}

		Identifier dimension = client.level.dimension().identifier();
		double x = client.player.getX();
		double y = client.player.getY();
		double z = client.player.getZ();

		List<Line> lines = new ArrayList<>(12);

		lines.add(new Line(String.format(Locale.ROOT, "XYZ %.2f / %.2f / %.2f", x, y, z), TEXT_COLOR, false));

		if (config.showChunk) {
			int blockX = (int) Math.floor(x);
			int blockZ = (int) Math.floor(z);
			lines.add(new Line(String.format(
					Locale.ROOT,
					"Block %d %d %d   Chunk %d %d",
					blockX,
					(int) Math.floor(y),
					blockZ,
					Math.floorDiv(blockX, 16),
					Math.floorDiv(blockZ, 16)
			), TEXT_COLOR, false));
		}

		lines.add(new Line("Dim " + dimension, TEXT_COLOR, false));

		float yaw = Mth.wrapDegrees(client.player.getYRot());

		if (config.showFacing) {
			lines.add(new Line(String.format(
					Locale.ROOT,
					"Facing %s  %d\u00B0",
					compassName(yaw),
					Math.round(yaw + 360.0f) % 360
			), TEXT_COLOR, false));
		}

		if (config.showBiome) {
			String biome = client.level.getBiome(client.player.blockPosition()).unwrapKey()
					.map(key -> prettify(key.identifier().getPath()))
					.orElse("Unknown");
			lines.add(new Line("Biome " + biome, TEXT_COLOR, false));
		}

		if (config.showGameTime) {
			long ticks = Math.floorMod(client.level.getOverworldClockTime(), 24000L);
			int hours = (int) ((ticks / 1000L + 6L) % 24L);
			int minutes = (int) ((ticks % 1000L) * 60L / 1000L);
			String weather = client.level.isThundering() ? "Storm" : client.level.isRaining() ? "Rain" : "Clear";

			lines.add(new Line(String.format(
					Locale.ROOT,
					"Time %02d:%02d   %s",
					hours,
					minutes,
					weather
			), TEXT_COLOR, false));
		}

		if (config.showLight) {
			int light = client.level.getMaxLocalRawBrightness(client.player.blockPosition());
			lines.add(new Line("Light " + light, TEXT_COLOR, false));
		}

		if (config.showSpeed) {
			double speed = client.player.getDeltaMovement().horizontalDistance() * 20.0;
			lines.add(new Line(String.format(Locale.ROOT, "Speed %.1f b/s", speed), TEXT_COLOR, false));
		}

		if (config.showFps && config.showClock) {
			lines.add(new Line("FPS " + client.getFps() + "   " + LocalTime.now().format(CLOCK), TEXT_COLOR, false));
		} else if (config.showFps) {
			lines.add(new Line("FPS " + client.getFps(), TEXT_COLOR, false));
		} else if (config.showClock) {
			lines.add(new Line(LocalTime.now().format(CLOCK), TEXT_COLOR, false));
		}

		if (config.showDurability) {
			ItemStack held = client.player.getMainHandItem();

			if (!held.isEmpty() && held.isDamageableItem()) {
				int max = held.getMaxDamage();
				int remaining = max - held.getDamageValue();
				float fraction = max > 0 ? remaining / (float) max : 0.0f;
				int color = fraction > 0.4f ? 0xFF55FF55 : fraction > 0.15f ? 0xFFFFD24A : 0xFFFF5555;

				lines.add(new Line(held.getHoverName().getString() + "  " + remaining + " / " + max, color, false));
			}
		}

		if (config.showWaypoints && config.maxWaypoints > 0) {
			config.waypoints.stream()
					.filter(waypoint -> waypoint.visible)
					.filter(waypoint -> waypoint.dimensionId().equals(dimension))
					.sorted(Comparator.comparingDouble(waypoint -> waypoint.distanceTo(x, y, z)))
					.limit(config.maxWaypoints)
					.forEach(waypoint -> lines.add(new Line(
							waypoint.name + "  " + formatDistance(waypoint.distanceTo(x, y, z)),
							0xFF000000 | (waypoint.color & 0xFFFFFF),
							true
					)));
		}

		int width = 0;

		for (Line line : lines) {
			width = Math.max(width, client.font.width(line.text) + (line.marker ? 10 : 0));
		}

		int left = PADDING;
		int top = PADDING;

		graphics.fill(
				left - PADDING,
				top - PADDING,
				left + width + PADDING,
				top + lines.size() * LINE_HEIGHT + PADDING - 1,
				BACKDROP_COLOR
		);

		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			int lineY = top + i * LINE_HEIGHT;

			if (line.marker) {
				graphics.fill(left, lineY + 2, left + 6, lineY + 8, line.color);
			}

			graphics.text(
					client.font,
					line.text,
					left + (line.marker ? 10 : 0),
					lineY,
					line.marker ? TEXT_COLOR : line.color,
					true
			);
		}
	}

	/**
	 * Cardinal name for a Minecraft yaw (0 = south, 90 = west, 180 = north, 270 = east).
	 */
	private static String compassName(float yaw) {
		int index = Math.round(Mth.wrapDegrees(yaw) / 45.0f);
		return COMPASS[Math.floorMod(index, COMPASS.length)];
	}

	/**
	 * Turns {@code plains} / {@code snowy_taiga} into {@code Plains} / {@code Snowy Taiga}.
	 */
	private static String prettify(String path) {
		String[] parts = path.split("_");
		StringBuilder builder = new StringBuilder(path.length());

		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}

		return builder.isEmpty() ? path : builder.toString();
	}

	static String formatDistance(double distance) {
		if (distance < 1000.0) {
			return Math.round(distance) + "m";
		}

		return String.format(Locale.ROOT, "%.1fkm", distance / 1000.0);
	}

	private record Line(String text, int color, boolean marker) {
	}
}
