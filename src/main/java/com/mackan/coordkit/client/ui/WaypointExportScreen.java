package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointExports;
import com.mackan.coordkit.client.WaypointQuery;
import com.mackan.coordkit.client.WaypointScope;
import com.mackan.coordkit.client.WaypointSharing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chooses a waypoint set and writes it to the exports folder, the clipboard,
 * or both.
 */
public final class WaypointExportScreen extends Screen {
	private static final int GAP = 4;
	private static final int MESSAGE_COLOR = 0xFF55FF55;
	private static final int ERROR_COLOR = 0xFFFF5555;
	private static final int MUTED_COLOR = 0xFFA0A0A0;

	private final WaypointManagerScreen parent;
	private ExportSet set = ExportSet.ALL;
	private ExportFormat format = ExportFormat.JSON;
	private Button setButton;
	private Button formatButton;
	private Button exportButton;
	private Path lastExport;
	private String message = "";
	private int messageColor = MESSAGE_COLOR;

	public WaypointExportScreen(WaypointManagerScreen parent) {
		super(Component.literal("Export Waypoints"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		int width = Math.min(300, this.width - 20);
		int left = Math.max(10, (this.width - width) / 2);

		this.setButton = addRenderableWidget(Button.builder(setLabel(), button -> {
			this.set = this.set.next();
			updateSummary();
		}).bounds(left, 38, width, 20).build());
		this.setButton.setTooltip(Tooltip.create(Component.literal("Change which waypoints are exported")));

		this.formatButton = addRenderableWidget(Button.builder(formatLabel(), button -> {
			this.format = this.format.next();
			updateSummary();
		}).bounds(left, 62, width, 20).build());
		this.formatButton.setTooltip(Tooltip.create(Component.literal(
				this.format == ExportFormat.JSON
						? "Writes a file and copies the same JSON"
						: "Copies one CK1 code per line")));

		int bottom = this.height - 28;
		int half = (width - GAP) / 2;

		addRenderableWidget(Button.builder(
				Component.literal("Back"),
				button -> onClose()
		).bounds(left, bottom, half, 20).build());

		this.exportButton = addRenderableWidget(Button.builder(
				Component.literal("Export"),
				button -> export()
		).bounds(left + half + GAP, bottom, half, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Open folder"),
				button -> openFolder()
		).bounds(left, bottom - 24, width, 20).build());

		updateSummary();
	}

	private void updateSummary() {
		if (this.setButton == null || this.formatButton == null || this.exportButton == null) {
			return;
		}

		this.setButton.setMessage(setLabel());
		this.formatButton.setMessage(formatLabel());
		this.formatButton.setTooltip(Tooltip.create(Component.literal(
				this.format == ExportFormat.JSON
						? "Writes a file and copies the same JSON"
						: "Copies one CK1 code per line")));
		this.exportButton.active = !selectedWaypoints().isEmpty();
	}

	private Component setLabel() {
		return Component.literal("Set: " + this.set.label);
	}

	private Component formatLabel() {
		return Component.literal("Format: " + this.format.label);
	}

	private List<Waypoint> selectedWaypoints() {
		WaypointQuery.ExportSet querySet = switch (this.set) {
			case ALL -> WaypointQuery.ExportSet.ALL;
			case VISIBLE_HERE -> WaypointQuery.ExportSet.VISIBLE_HERE;
			case VISIBLE_ONLY -> WaypointQuery.ExportSet.VISIBLE_ONLY;
		};

		return WaypointQuery.exportSet(
				CoordKitConfig.get().waypoints,
				querySet,
				WaypointScope.currentWorldKey(this.minecraft)
		);
	}

	private void export() {
		List<Waypoint> selected = selectedWaypoints();

		if (selected.isEmpty()) {
			showError("No waypoints in this set.");
			return;
		}

		if (this.format == ExportFormat.SHARE_CODES) {
			copyShareCodes(selected);
		} else {
			exportJson(selected);
		}
	}

	private void exportJson(List<Waypoint> selected) {
		String json = WaypointSharing.exportJson(selected);
		Path file;

		try {
			file = WaypointExports.writeJson(WaypointExports.directory(), json, LocalDateTime.now());
		} catch (IOException | RuntimeException e) {
			showError("Could not write the export file: " + errorText(e));
			ClientFeedback.chat(this.minecraft, "CoordKit export failed: " + errorText(e));
			return;
		}

		this.lastExport = file;

		try {
			this.minecraft.keyboardHandler.setClipboard(json);
		} catch (RuntimeException e) {
			showError("Saved the file, but could not copy the JSON: " + errorText(e));
			ClientFeedback.chat(this.minecraft, "CoordKit exported " + selected.size()
					+ " waypoints to " + file + " but could not copy the JSON: " + errorText(e));
			return;
		}

		showMessage("Exported " + selected.size() + " waypoint"
				+ (selected.size() == 1 ? "" : "s") + " and copied the JSON.");
		ClientFeedback.actionBar(this.minecraft, "Exported " + selected.size()
				+ (selected.size() == 1 ? " waypoint" : " waypoints"));
		ClientFeedback.chat(this.minecraft, "CoordKit exported " + selected.size()
				+ " waypoints to " + file);
	}

	private void copyShareCodes(List<Waypoint> selected) {
		String codes = WaypointSharing.exportShareCodes(selected);

		try {
			this.minecraft.keyboardHandler.setClipboard(codes);
		} catch (RuntimeException e) {
			showError("Could not copy the share codes: " + errorText(e));
			ClientFeedback.chat(this.minecraft, "CoordKit could not copy the share codes: " + errorText(e));
			return;
		}

		showMessage("Copied " + selected.size() + " share code"
				+ (selected.size() == 1 ? "" : "s") + " to the clipboard.");
		ClientFeedback.actionBar(this.minecraft, "Copied " + selected.size()
				+ (selected.size() == 1 ? " share code" : " share codes"));

		if (selected.stream().anyMatch(waypoint -> WaypointSharing.shareCode(waypoint).length() > 240)) {
			ClientFeedback.chat(this.minecraft,
					"One or more share codes are over 240 characters, too long for one chat message."
					+ " Paste them in Discord or a file instead.");
		}
	}

	private void openFolder() {
		try {
			Path directory = WaypointExports.directory();
			Files.createDirectories(directory);
			Util.getPlatform().openPath(directory);
		} catch (IOException | RuntimeException e) {
			showError("Could not open the exports folder: " + errorText(e));
		}
	}

	private void showMessage(String text) {
		this.message = text;
		this.messageColor = MESSAGE_COLOR;
	}

	private void showError(String text) {
		this.message = text;
		this.messageColor = ERROR_COLOR;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
		graphics.centeredText(this.font,
				Component.literal("Choose a set and format, then export."),
				this.width / 2, 22, MUTED_COLOR);

		int width = Math.min(300, this.width - 20);
		int left = Math.max(10, (this.width - width) / 2);
		int count = selectedWaypoints().size();

		graphics.text(this.font,
				count == 0 ? "No waypoints in this set." : count + (count == 1 ? " waypoint" : " waypoints") + " selected.",
				left, 88, count == 0 ? ERROR_COLOR : 0xFFFFFFFF, false);

		graphics.text(this.font,
				this.format == ExportFormat.JSON
						? "Writes a file and copies the JSON."
						: "Copies one CK1 code per line.",
				left, 100, MUTED_COLOR, false);

		if (this.lastExport != null) {
			graphics.text(this.font, "File:", left, 116, MUTED_COLOR, false);
			int y = 128;

			for (FormattedCharSequence line : this.font.split(Component.literal(this.lastExport.toString()), width)) {
				if (y > 152) {
					break;
				}

				graphics.text(this.font, line, left, y, 0xFFFFFFFF, false);
				y += 12;
			}
		}

		drawWrapped(graphics, this.message, left, this.height - 74, width, this.messageColor, 2);
	}

	private void drawWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int width,
			int color, int maxLines) {
		if (text == null || text.isEmpty()) {
			return;
		}

		List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);

		for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
			graphics.text(this.font, lines.get(i), x, y + i * 12, color, false);
		}
	}

	private static String errorText(Throwable error) {
		String message = error.getMessage();
		return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
	}

	@Override
	public void onClose() {
		if (this.parent != null) {
			this.minecraft.gui.setScreen(this.parent);
		} else {
			super.onClose();
		}
	}

	private enum ExportSet {
		ALL("All waypoints"),
		VISIBLE_HERE("Visible here"),
		VISIBLE_ONLY("Visible only");

		private final String label;

		ExportSet(String label) {
			this.label = label;
		}

		ExportSet next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private enum ExportFormat {
		JSON("JSON file + clipboard"),
		SHARE_CODES("Share codes");

		private final String label;

		ExportFormat(String label) {
			this.label = label;
		}

		ExportFormat next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}
}
