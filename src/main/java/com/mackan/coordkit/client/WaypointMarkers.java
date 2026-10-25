package com.mackan.coordkit.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen-space markers for saved waypoints.
 *
 * <p>26.2 has no world-render event, so the markers are projected from the
 * camera to the HUD layer instead. They are drawn after the world, which means
 * they stay visible through terrain and are exactly where you would expect
 * when you look around. Waypoints outside the current view collapse to a clean
 * edge arrow with no label.</p>
 */
public final class WaypointMarkers {
	private static final double MIN_DISTANCE = 1.5;
	private static final int EDGE_MARGIN = 14;
	private static final int LABEL_LINE_HEIGHT = 11;
	private static final int LABEL_GAP = 4;

	private WaypointMarkers() {
	}

	static void render(GuiGraphicsExtractor graphics, Minecraft client, CoordKitConfig config) {
		if (client.player == null || client.level == null) {
			return;
		}

		Camera camera = client.gameRenderer.mainCamera();

		if (camera == null || !camera.isInitialized()) {
			return;
		}

		Identifier dimension = client.level.dimension().identifier();
		String currentWorld = WaypointScope.currentWorldKey(client);
		Vec3 cameraPos = camera.position();
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();

		if (screenWidth <= 0 || screenHeight <= 0) {
			return;
		}

		List<Marker> markers = new ArrayList<>();

		for (Waypoint waypoint : config.waypoints) {
			if (!waypoint.visible || !waypoint.dimensionId().equals(dimension)
					|| !WaypointScope.isVisibleIn(waypoint, currentWorld)) {
				continue;
			}

			double dx = waypoint.x - cameraPos.x;
			double dy = waypoint.y - cameraPos.y;
			double dz = waypoint.z - cameraPos.z;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

			if (distance < MIN_DISTANCE || distance > config.markerMaxDistance) {
				continue;
			}

			markers.add(new Marker(waypoint, dx, dy, dz, distance));
		}

		if (markers.isEmpty()) {
			return;
		}

		// Far markers first so nearer ones overlap them.
		markers.sort(Comparator.comparingDouble(Marker::distance).reversed());

		float fov = camera.getFov();

		if (fov <= 1.0f) {
			fov = 70.0f;
		}

		double aspect = (double) screenWidth / (double) screenHeight;
		double tanHalfY = Math.tan(Math.toRadians(fov / 2.0));
		double halfFovY = fov / 2.0;
		double halfFovX = Math.toDegrees(Math.atan(tanHalfY * aspect));
		Vec3 forward = new Vec3(camera.forwardVector());
		double edgeNdcX = 1.0 - 2.0 * EDGE_MARGIN / screenWidth;
		double edgeNdcY = 1.0 - 2.0 * EDGE_MARGIN / screenHeight;
		List<Bounds> labels = new ArrayList<>();

		for (Marker marker : markers) {
			// Use the exact matrix the world is rendered with, so the marker sticks
			// to the waypoint even with camera bobbing, roll and FOV effects.
			Vec3 ndc = client.gameRenderer.projectPointToScreen(
					new Vec3(marker.waypoint.x, marker.waypoint.y, marker.waypoint.z));
			double forwardDot = marker.dx * forward.x + marker.dy * forward.y + marker.dz * forward.z;
			boolean inFront = forwardDot > 0.0;

			// Fade distant markers out slightly so near ones stand out.
			double fade = Mth.clamp(marker.distance / config.markerMaxDistance, 0.0, 1.0);
			int alpha = (int) Math.round(255.0 - 80.0 * fade);
			int rgb = marker.waypoint.color & 0xFFFFFF;
			int color = (alpha << 24) | rgb;
			int shadow = ((int) (alpha * 0.65) << 24);

			boolean onScreen = inFront
					&& ndc.x >= -1.0 && ndc.x <= 1.0
					&& ndc.y >= -1.0 && ndc.y <= 1.0;

			if (onScreen) {
				int x = (int) Math.round((ndc.x + 1.0) * 0.5 * screenWidth);
				int y = (int) Math.round((1.0 - ndc.y) * 0.5 * screenHeight);

				drawDiamond(graphics, x, y, 4, shadow);
				drawDiamond(graphics, x, y, 3, color);

				drawLabel(graphics, client, marker, x + 9, y - 4, rgb, alpha,
						screenWidth, screenHeight, labels);
			} else if (config.markerOffscreenArrows) {
				double nx;
				double ny;

				if (inFront) {
					// Just outside the view: the projected NDC already points the right way.
					nx = ndc.x;
					ny = -ndc.y;
				} else {
					// Behind the camera the perspective divide flips signs, so fall
					// back to the relative angles for the arrow direction.
					double horizontal = Math.sqrt(marker.dx * marker.dx + marker.dz * marker.dz);
					double targetYaw = Math.toDegrees(Math.atan2(-marker.dx, marker.dz));
					double targetPitch = -Math.toDegrees(Math.atan2(marker.dy, horizontal));
					nx = Mth.wrapDegrees(targetYaw - camera.yRot()) / halfFovX;
					ny = Mth.wrapDegrees(targetPitch - camera.xRot()) / halfFovY;
				}

				double scale = Math.max(Math.abs(nx) / edgeNdcX, Math.abs(ny) / edgeNdcY);

				if (scale > 1.0) {
					nx /= scale;
					ny /= scale;
				}

				int x = (int) Math.round(screenWidth / 2.0 + nx * (screenWidth / 2.0 - EDGE_MARGIN));
				int y = (int) Math.round(screenHeight / 2.0 + ny * (screenHeight / 2.0 - EDGE_MARGIN));

				boolean horizontalArrow = Math.abs(nx) > Math.abs(ny);
				int direction = horizontalArrow ? (nx > 0 ? 1 : -1) : (ny > 0 ? 1 : -1);

				drawTriangle(graphics, x + 1, y + 1, direction, horizontalArrow, shadow);
				drawTriangle(graphics, x, y, direction, horizontalArrow, color);
			}
		}
	}

	/**
	 * Diamond marker drawn as horizontal slices: 1, 3, 5, 7, 5, 3, 1 pixels.
	 */
	private static void drawDiamond(GuiGraphicsExtractor graphics, int x, int y, int radius, int color) {
		for (int offset = -radius; offset <= radius; offset++) {
			int half = radius - Math.abs(offset);
			graphics.fill(x - half, y + offset, x + half + 1, y + offset + 1, color);
		}
	}

	/**
	 * Small filled triangle used for off-screen directions.
	 */
	private static void drawTriangle(GuiGraphicsExtractor graphics, int x, int y,
			int direction, boolean horizontal, int color) {
		for (int i = 0; i < 5; i++) {
			int along = direction * (i - 2);
			int half = 5 - i;

			if (horizontal) {
				graphics.fill(x + along, y - half + 1, x + along + 1, y + half, color);
			} else {
				graphics.fill(x - half + 1, y + along, x + half, y + along + 1, color);
			}
		}
	}

	private static void drawLabel(GuiGraphicsExtractor graphics, Minecraft client, Marker marker,
			int x, int y, int rgb, int alpha, int screenWidth, int screenHeight, List<Bounds> labels) {
		String name = marker.waypoint.name;
		String distance = CoordKitHud.formatDistance(marker.distance);
		int nameWidth = client.font.width(name);
		int distanceWidth = client.font.width(distance);
		int width = nameWidth + LABEL_GAP + distanceWidth;
		int labelX = Mth.clamp(x, 2, Math.max(2, screenWidth - width - 4));
		int labelY = resolveLabelY(y, labelX, width, screenHeight, labels);

		labels.add(new Bounds(labelX - 2, labelY - 1, width + 4, LABEL_LINE_HEIGHT));

		graphics.fill(labelX - 2, labelY - 1, labelX + width + 2, labelY + LABEL_LINE_HEIGHT - 2,
				(int) (alpha * 0.30) << 24);
		graphics.text(client.font, name, labelX, labelY, (alpha << 24) | 0xFFFFFF, true);
		graphics.text(client.font, distance, labelX + nameWidth + LABEL_GAP, labelY,
				(int) (alpha * 0.85) << 24 | rgb, true);
	}

	/**
	 * Keeps labels readable when waypoints are close together: tries the natural
	 * spot, then shifts down/up by a line.
	 */
	private static int resolveLabelY(int desiredY, int x, int width, int screenHeight, List<Bounds> labels) {
		int height = LABEL_LINE_HEIGHT - 2;
		int maxY = Math.max(2, screenHeight - height - 2);
		int[] offsets = {0, 1, -1, 2, -2};

		for (int offset : offsets) {
			int candidate = Mth.clamp(desiredY + offset * LABEL_LINE_HEIGHT, 2, maxY);

			if (!collides(labels, x - 2, candidate - 1, width + 4, LABEL_LINE_HEIGHT)) {
				return candidate;
			}
		}

		return Mth.clamp(desiredY, 2, maxY);
	}

	private static boolean collides(List<Bounds> labels, int x, int y, int width, int height) {
		for (Bounds bounds : labels) {
			if (x < bounds.x + bounds.width && x + width > bounds.x
					&& y < bounds.y + bounds.height && y + height > bounds.y) {
				return true;
			}
		}

		return false;
	}

	private record Marker(Waypoint waypoint, double dx, double dy, double dz, double distance) {
	}

	private record Bounds(int x, int y, int width, int height) {
	}
}
