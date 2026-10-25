package com.mackan.coordkit.client;

import com.mackan.coordkit.CoordKit;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * File storage for waypoint exports.
 *
 * <p>The write and lookup methods take an explicit directory so they can be
 * tested with a temporary folder; {@link #directory()} is the only
 * client-facing piece.</p>
 */
public final class WaypointExports {
	private static final String PREFIX = "coordkit-";
	private static final String SUFFIX = ".json";
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private WaypointExports() {
	}

	/**
	 * {@code config/coordkit/exports}.
	 */
	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(CoordKit.MOD_ID)
				.resolve("exports");
	}

	/**
	 * Writes a timestamped JSON export, never overwriting an existing file.
	 */
	public static Path writeJson(Path directory, String json, LocalDateTime now) throws IOException {
		Files.createDirectories(directory);
		Path file = uniqueFile(directory, now);
		Files.writeString(file, json, StandardCharsets.UTF_8);
		return file;
	}

	static Path uniqueFile(Path directory, LocalDateTime now) {
		String base = PREFIX + TIMESTAMP.format(now);
		int suffix = 1;
		Path file = directory.resolve(base + SUFFIX);

		while (Files.exists(file)) {
			suffix++;
			file = directory.resolve(base + "-" + suffix + SUFFIX);
		}

		return file;
	}

	/**
	 * The most recently modified export in the folder, if there is one.
	 */
	public static Optional<Path> latest(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			return Optional.empty();
		}

		try (Stream<Path> files = Files.list(directory)) {
			return files
					.filter(Files::isRegularFile)
					.filter(WaypointExports::isExport)
					.max(Comparator
							.comparingLong(WaypointExports::modified)
							.thenComparing(file -> file.getFileName().toString()));
		}
	}

	private static boolean isExport(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
	}

	private static long modified(Path file) {
		try {
			return Files.getLastModifiedTime(file).toMillis();
		} catch (IOException e) {
			return Long.MIN_VALUE;
		}
	}
}
