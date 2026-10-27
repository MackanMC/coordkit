package com.mackan.coordkit.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Transient ruler: press once for point A, aim and press again for point B,
 * then press a third time to clear.
 *
 * <p>The line is sampled in world space and projected with the same camera
 * matrices as the waypoint markers, so it follows perspective instead of
 * looking like a flat 2D overlay.</p>
 */
public final class MeasureOverlay {
	private static final int PADDING = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int BACKDROP_COLOR = 0x90000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int MUTED_COLOR = 0xFFA0A0A0;
	private static final int POINT_A_COLOR = 0xFF46D0C6;
	private static final int POINT_B_COLOR = 0xFFF2D64B;
	private static final int LINE_COLOR = 0xC0FFFFFF;
	private static final int ENDPOINT_MARGIN = 6;
	private static final double SAMPLE_STEP = 0.75;
	private static final int MAX_SAMPLES = 400;

	private static final MeasureState STATE = new MeasureState();

	private MeasureOverlay() {
	}

	/**
	 * Handles one press of the measure key.
	 */
	public static void press(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}

		Target target = currentTarget(client);
		String dimension = client.level.dimension().identifier().toString();
		MeasureState.Result result = STATE.press(target.point(), dimension);

		switch (result) {
			case SET_A -> ClientFeedback.actionBar(client,
					target.atBlock() ? "Measure A set" : "Measure A set at your feet");
			case SET_B -> {
				MeasurePoint a = STATE.a();
				MeasurePoint b = STATE.b();

				if (a != null && b != null) {
					ClientFeedback.actionBar(client,
							"Measure B set: " + CoordKitHud.formatDistance(MeasureMath.distance(a, b)) + " (" + blocksBetween(a, b) + " blocks between)");
					sendCopyActions(client, a, b);
				}
			}
			case CLEARED -> ClientFeedback.actionBar(client, "Measure cleared");
		}
	}

	/**
	 * Clears a measurement that no longer belongs to the current dimension.
	 */
	public static void tick(Minecraft client) {
		if (!STATE.active()) {
			return;
		}

		if (client.player == null || client.level == null) {
			STATE.clear();
			return;
		}

		if (!STATE.keepInDimension(client.level.dimension().identifier().toString())) {
			ClientFeedback.actionBar(client, "Measure cleared (dimension changed)");
		}
	}

	static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		if (!STATE.active() || client.player == null || client.level == null) {
			return;
		}

		MeasurePoint a = STATE.a();

		if (a == null) {
			return;
		}

		Camera camera = client.gameRenderer.mainCamera();

		if (camera == null || !camera.isInitialized()) {
			return;
		}

		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();

		if (screenWidth <= 0 || screenHeight <= 0) {
			return;
		}

		boolean preview = !STATE.complete();
		MeasurePoint b = preview ? currentTarget(client).point() : STATE.b();

		if (b == null) {
			return;
		}

		drawLine(graphics, client, camera, a, b, screenWidth, screenHeight);
		drawEndpoint(graphics, client, camera, a, "A", POINT_A_COLOR, screenWidth, screenHeight);
		drawEndpoint(graphics, client, camera, b, "B", POINT_B_COLOR, screenWidth, screenHeight);
		drawPanel(graphics, client, a, b, preview);
	}

	private static void drawLine(GuiGraphicsExtractor graphics, Minecraft client, Camera camera,
			MeasurePoint a, MeasurePoint b, int screenWidth, int screenHeight) {
		double distance = MeasureMath.distance(a, b);
		int samples = Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP)));

		for (int i = 0; i <= samples; i++) {
			double t = (double) i / samples;
			MeasurePoint point = new MeasurePoint(
					a.x() + (b.x() - a.x()) * t,
					a.y() + (b.y() - a.y()) * t,
					a.z() + (b.z() - a.z()) * t
			);
			ScreenProjection projection = ScreenProjection.project(
					client, camera, new Vec3(point.x(), point.y(), point.z()));

			if (!projection.onScreen()) {
				continue;
			}

			int x = projection.screenX(screenWidth);
			int y = projection.screenY(screenHeight);
			graphics.fill(x, y, x + 2, y + 2, LINE_COLOR);
		}
	}

	private static void drawEndpoint(GuiGraphicsExtractor graphics, Minecraft client, Camera camera,
			MeasurePoint point, String label, int color, int screenWidth, int screenHeight) {
		ScreenPoint screen = endpointPosition(client, camera, point, screenWidth, screenHeight);
		int labelX = Mth.clamp(screen.x() + 6, 2, Math.max(2, screenWidth - client.font.width(label) - 2));
		int labelY = Mth.clamp(screen.y() - 4, 2, Math.max(2, screenHeight - 11));

		drawDiamond(graphics, screen.x(), screen.y(), 4, 0xA0000000);
		drawDiamond(graphics, screen.x(), screen.y(), 3, color);
		graphics.text(client.font, label, labelX, labelY, color, true);
	}

	/**
	 * Screen position for an endpoint, clamped to the edge when it is outside
	 * the view. Points behind the camera use their relative yaw and pitch so the
	 * marker still points the player towards them.
	 */
	private static ScreenPoint endpointPosition(Minecraft client, Camera camera, MeasurePoint point,
			int screenWidth, int screenHeight) {
		ScreenProjection projection = ScreenProjection.project(
				client, camera, new Vec3(point.x(), point.y(), point.z()));

		if (projection.inFront()) {
			int x = Mth.clamp(projection.screenX(screenWidth), ENDPOINT_MARGIN, screenWidth - ENDPOINT_MARGIN);
			int y = Mth.clamp(projection.screenY(screenHeight), ENDPOINT_MARGIN, screenHeight - ENDPOINT_MARGIN);
			return new ScreenPoint(x, y);
		}

		Vec3 cameraPosition = camera.position();
		double dx = point.x() - cameraPosition.x;
		double dy = point.y() - cameraPosition.y;
		double dz = point.z() - cameraPosition.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double targetPitch = -Math.toDegrees(Math.atan2(dy, horizontal));
		float fov = camera.getFov();

		if (fov <= 1.0f) {
			fov = 70.0f;
		}

		double halfFovY = fov / 2.0;
		double aspect = (double) screenWidth / (double) screenHeight;
		double halfFovX = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(halfFovY)) * aspect));
		double nx = Mth.wrapDegrees(targetYaw - camera.yRot()) / halfFovX;
		double ny = Mth.wrapDegrees(targetPitch - camera.xRot()) / halfFovY;
		double scale = Math.max(Math.abs(nx), Math.abs(ny));

		if (scale > 1.0) {
			nx /= scale;
			ny /= scale;
		}

		int x = (int) Math.round(screenWidth / 2.0 + nx * (screenWidth / 2.0 - ENDPOINT_MARGIN));
		int y = (int) Math.round(screenHeight / 2.0 + ny * (screenHeight / 2.0 - ENDPOINT_MARGIN));
		return new ScreenPoint(
				Mth.clamp(x, ENDPOINT_MARGIN, screenWidth - ENDPOINT_MARGIN),
				Mth.clamp(y, ENDPOINT_MARGIN, screenHeight - ENDPOINT_MARGIN)
		);
	}

	private static void drawPanel(GuiGraphicsExtractor graphics, Minecraft client,
			MeasurePoint a, MeasurePoint b, boolean preview) {
		MeasurePoint midpoint = MeasureMath.midpoint(a, b);
		String[] lines = {
				preview ? "Measure   A \u2192 aim" : "Measure   A \u2192 B",
				String.format(Locale.ROOT, "%.1f m   horizontal %.1f m",
						MeasureMath.distance(a, b), MeasureMath.horizontalDistance(a, b)),
				String.format(Locale.ROOT, "\u0394  %+.1f / %+.1f / %+.1f",
						MeasureMath.deltaX(a, b), MeasureMath.deltaY(a, b), MeasureMath.deltaZ(a, b)),
				String.format(Locale.ROOT, "Mid  %.1f / %.1f / %.1f",
						midpoint.x(), midpoint.y(), midpoint.z()),
				preview ? "" : (blocksBetween(a, b) + " blocks between")
		};

		int width = 0;

		for (String line : lines) {
			if (line.isEmpty()) {
				continue;
			}
			width = Math.max(width, client.font.width(line));
		}

		int visibleLines = 0;
		for (String line : lines) {
			if (!line.isEmpty()) {
				visibleLines++;
			}
		}

		int panelWidth = width + PADDING * 2;
		int panelHeight = visibleLines * LINE_HEIGHT + PADDING * 2 - 2;
		int left = Math.max(PADDING, graphics.guiWidth() - panelWidth - PADDING);
		int top = PADDING;

		graphics.fill(left, top, left + panelWidth, top + panelHeight, BACKDROP_COLOR);

		int lineOffset = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.isEmpty()) {
				continue;
			}
			int color = i == 0 ? TEXT_COLOR : (i == 1 ? POINT_A_COLOR : (i == 4 ? POINT_B_COLOR : MUTED_COLOR));
			graphics.text(client.font, line, left + PADDING, top + PADDING + lineOffset * LINE_HEIGHT,
					color, true);
			lineOffset++;
		}
	}

	private static void sendCopyActions(Minecraft client, MeasurePoint a, MeasurePoint b) {
		MeasurePoint midpoint = MeasureMath.midpoint(a, b);
		MutableComponent message = Component.literal(
				"Measured " + CoordKitHud.formatDistance(MeasureMath.distance(a, b)) + "  ");
		message.append(copyAction("Copy A", a));
		message.append(Component.literal("  "));
		message.append(copyAction("Copy B", b));
		message.append(Component.literal("  "));
		message.append(copyAction("Copy midpoint", midpoint));
		ClientFeedback.chat(client, message);
	}

	private static MutableComponent copyAction(String label, MeasurePoint point) {
		String command = String.format(Locale.ROOT, "/tp @s %.2f %.2f %.2f",
				point.x(), point.y(), point.z());

		return Component.literal("[" + label + "]").withStyle(style -> style
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.CopyToClipboard(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy " + command))));
	}

	/**
	 * Number of blocks between two block centres. If A is the centre of block X
	 * and B is the centre of block X+1, the distance between centres is 1.0 and
	 * there is 1 block between them. Diagonals only count full blocks along each
	 * axis (floor of the axis delta), matching the request to count blocks
	 * between the points rather than the centre distance.
	 */
	private static long blocksBetween(MeasurePoint a, MeasurePoint b) {
		long dx = (long) Math.floor(Math.abs(b.x() - a.x()));
		long dy = (long) Math.floor(Math.abs(b.y() - a.y()));
		long dz = (long) Math.floor(Math.abs(b.z() - a.z()));
		return dx + dy + dz;
	}

	private static Target currentTarget(Minecraft client) {
		Vec3 block = WaypointActions.crosshairTarget(client);

		if (block != null) {
			return new Target(new MeasurePoint(block.x, block.y, block.z), true);
		}

		Vec3 feet = client.player.position();
		return new Target(new MeasurePoint(feet.x, feet.y, feet.z), false);
	}

	private static void drawDiamond(GuiGraphicsExtractor graphics, int x, int y, int radius, int color) {
		for (int offset = -radius; offset <= radius; offset++) {
			int half = radius - Math.abs(offset);
			graphics.fill(x - half, y + offset, x + half + 1, y + offset + 1, color);
		}
	}

	private record Target(MeasurePoint point, boolean atBlock) {
	}

	private record ScreenPoint(int x, int y) {
	}
}
