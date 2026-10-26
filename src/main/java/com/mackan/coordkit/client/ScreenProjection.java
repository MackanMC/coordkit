package com.mackan.coordkit.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * A world position projected onto the screen with the camera's current
 * matrices.
 *
 * <p>{@link #ndcX()} and {@link #ndcY()} are normalised device coordinates:
 * {@code -1} is the left/top edge and {@code 1} is the right/bottom edge.
 * {@link #inFront()} is computed separately because the perspective divide
 * also flips the projection for points behind the camera.</p>
 */
public record ScreenProjection(double ndcX, double ndcY, boolean inFront) {
	public static ScreenProjection project(Minecraft client, Camera camera, Vec3 worldPosition) {
		Vec3 ndc = client.gameRenderer.projectPointToScreen(worldPosition);
		Vec3 cameraPosition = camera.position();
		Vec3 forward = new Vec3(camera.forwardVector());
		double dx = worldPosition.x - cameraPosition.x;
		double dy = worldPosition.y - cameraPosition.y;
		double dz = worldPosition.z - cameraPosition.z;
		boolean inFront = dx * forward.x + dy * forward.y + dz * forward.z > 0.0;

		return new ScreenProjection(ndc.x, ndc.y, inFront);
	}

	public boolean onScreen() {
		return inFront
				&& ndcX >= -1.0 && ndcX <= 1.0
				&& ndcY >= -1.0 && ndcY <= 1.0;
	}

	public int screenX(int screenWidth) {
		return (int) Math.round((ndcX + 1.0) * 0.5 * screenWidth);
	}

	public int screenY(int screenHeight) {
		return (int) Math.round((1.0 - ndcY) * 0.5 * screenHeight);
	}
}
