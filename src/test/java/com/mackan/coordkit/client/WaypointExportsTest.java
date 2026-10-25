package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the export folder naming, lookup and JSON round-trip.
 */
class WaypointExportsTest {
	private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 25, 20, 41, 31);

	@TempDir
	Path directory;

	@Test
	void writesTimestampedFilesWithoutOverwriting() throws IOException {
		Path first = WaypointExports.writeJson(this.directory, "first", TIME);
		Path second = WaypointExports.writeJson(this.directory, "second", TIME);

		assertEquals("coordkit-20261025-204131.json", first.getFileName().toString());
		assertEquals("coordkit-20261025-204131-2.json", second.getFileName().toString());
		assertEquals("first", Files.readString(first));
		assertEquals("second", Files.readString(second));
	}

	@Test
	void writesUtf8() throws IOException {
		String json = "{\"name\":\"Café ✓\"}";

		Path file = WaypointExports.writeJson(this.directory, json, TIME);

		assertEquals(json, Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	void latestUsesModificationTimeAndIgnoresOtherFiles() throws IOException {
		Path older = this.directory.resolve("coordkit-20261025-204131.json");
		Path newer = this.directory.resolve("coordkit-20261025-204132.json");
		Path notes = this.directory.resolve("notes.txt");
		Path notAFile = this.directory.resolve("coordkit-folder.json");

		Files.writeString(older, "older");
		Files.writeString(newer, "newer");
		Files.writeString(notes, "notes");
		Files.createDirectory(notAFile);
		Files.setLastModifiedTime(older, FileTime.fromMillis(1_000));
		Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000));

		Optional<Path> latest = WaypointExports.latest(this.directory);

		assertTrue(latest.isPresent());
		assertEquals(newer, latest.get());
	}

	@Test
	void latestOfMissingFolderIsEmpty() throws IOException {
		assertTrue(WaypointExports.latest(this.directory.resolve("missing")).isEmpty());
	}

	@Test
	void writtenJsonImportsBackUnchanged() throws IOException {
		Waypoint original = new Waypoint("Base ✓", 1.25, 64.0, -30.5, "minecraft:overworld", 0x123456);
		original.group = "Home";
		original.world = "sp:test";
		original.visible = false;
		original.autoDeath = true;

		String json = WaypointSharing.exportJson(List.of(original));
		Path file = WaypointExports.writeJson(this.directory, json, TIME);
		String written = Files.readString(file, StandardCharsets.UTF_8);

		assertEquals(json, written);

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				written, List.of(), "sp:test", WaypointSharing.ScopePolicy.KEEP);

		assertTrue(plan.canImport());
		assertEquals(1, plan.validCount());

		Waypoint imported = plan.valid().get(0);
		assertEquals("Base ✓", imported.name);
		assertEquals("Home", imported.group);
		assertEquals("sp:test", imported.world);
		assertEquals(1.25, imported.x);
		assertEquals(64.0, imported.y);
		assertEquals(-30.5, imported.z);
		assertEquals(0x123456, imported.color);
		assertFalse(imported.visible);
		assertFalse(imported.autoDeath);
	}
}
