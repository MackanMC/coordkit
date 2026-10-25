package com.mackan.coordkit;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for CoordKit.
 *
 * <p>CoordKit is a client-only mod, so this class intentionally has no
 * {@code ModInitializer}: nothing is loaded when the mod happens to sit in a
 * server's mods folder.</p>
 */
public final class CoordKit {
	public static final String MOD_ID = "coordkit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private CoordKit() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
