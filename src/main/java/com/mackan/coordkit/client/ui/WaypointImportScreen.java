package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointExports;
import com.mackan.coordkit.client.WaypointScope;
import com.mackan.coordkit.client.WaypointSharing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Reads waypoints from the clipboard or the latest export, previews the
 * result, and appends the valid entries.
 */
public final class WaypointImportScreen extends Screen {
	private static final int GAP = 4;
	private static final int ERROR_COLOR = 0xFFFF5555;
	private static final int MUTED_COLOR = 0xFFA0A0A0;
	private static final int WARNING_COLOR = 0xFFFFAA00;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private final WaypointManagerScreen parent;
	private WaypointSharing.ScopePolicy policy = WaypointSharing.ScopePolicy.KEEP;
	private String sourceText = "";
	private String sourceLabel = "Clipboard";
	private String readError;
	private WaypointSharing.ImportPlan plan;

	private Button policyButton;
	private Button importButton;

	public WaypointImportScreen(WaypointManagerScreen parent) {
		super(Component.literal("Import Waypoints"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		int width = Math.min(300, this.width - 20);
		int left = Math.max(10, (this.width - width) / 2);
		int bottom = this.height - 28;
		int half = (width - GAP) / 2;

		this.policyButton = addRenderableWidget(Button.builder(policyLabel(), button -> cyclePolicy())
				.bounds(left, bottom - 46, width, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Refresh clipboard"),
				button -> loadFromClipboard()
		).bounds(left, bottom - 22, half, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Import latest export"),
				button -> loadLatestExport()
		).bounds(left + half + GAP, bottom - 22, half, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Back"),
				button -> onClose()
		).bounds(left, bottom, half, 20).build());

		this.importButton = addRenderableWidget(Button.builder(
				Component.literal("Import"),
				button -> importWaypoints()
		).bounds(left + half + GAP, bottom, half, 20).build());

		loadFromClipboard();
	}

	private void loadFromClipboard() {
		this.sourceLabel = "Clipboard";

		try {
			String clipboard = this.minecraft.keyboardHandler.getClipboard();
			this.sourceText = clipboard == null ? "" : clipboard;
			this.readError = null;
		} catch (RuntimeException e) {
			this.sourceText = "";
			this.readError = "Could not read the clipboard: " + errorText(e);
		}

		reparse();
	}

	private void loadLatestExport() {
		try {
			Optional<Path> latest = WaypointExports.latest(WaypointExports.directory());

			if (latest.isEmpty()) {
				this.sourceText = "";
				this.sourceLabel = "Latest export";
				this.readError = "No exports found in the exports folder.";
			} else {
				this.sourceText = Files.readString(latest.get());
				this.sourceLabel = latest.get().getFileName().toString();
				this.readError = null;
			}
		} catch (IOException | RuntimeException e) {
			this.sourceText = "";
			this.sourceLabel = "Latest export";
			this.readError = "Could not read the latest export: " + errorText(e);
		}

		reparse();
	}

	private void reparse() {
		String currentWorld = currentWorldKey();

		if (this.policy == WaypointSharing.ScopePolicy.CURRENT_WORLD && currentWorld.isEmpty()) {
			this.policy = WaypointSharing.ScopePolicy.KEEP;
		}

		if (this.readError != null) {
			this.plan = null;
		} else {
			this.plan = WaypointSharing.importText(
					this.sourceText,
					CoordKitConfig.get().waypoints,
					currentWorld,
					this.policy
			);
		}

		updateButtons();
	}

	private void cyclePolicy() {
		if (currentWorldKey().isEmpty()) {
			this.policy = this.policy == WaypointSharing.ScopePolicy.KEEP
					? WaypointSharing.ScopePolicy.GLOBAL
					: WaypointSharing.ScopePolicy.KEEP;
		} else {
			this.policy = switch (this.policy) {
				case KEEP -> WaypointSharing.ScopePolicy.CURRENT_WORLD;
				case CURRENT_WORLD -> WaypointSharing.ScopePolicy.GLOBAL;
				case GLOBAL -> WaypointSharing.ScopePolicy.KEEP;
			};
		}

		reparse();
	}

	private Component policyLabel() {
		String label = switch (this.policy) {
			case KEEP -> "Keep original scope";
			case CURRENT_WORLD -> "Current world";
			case GLOBAL -> "Global";
		};

		return Component.literal("Scope: " + label);
	}

	private void updateButtons() {
		if (this.policyButton == null || this.importButton == null) {
			return;
		}

		this.policyButton.setMessage(policyLabel());
		this.policyButton.setTooltip(Tooltip.create(Component.literal(
				currentWorldKey().isEmpty()
						? "The current world is unknown here, so only Keep original and Global are available"
						: "Click to change how imported waypoints are scoped")));
		this.importButton.active = this.plan != null && this.plan.canImport();
		this.importButton.setTooltip(Tooltip.create(Component.literal(
				this.importButton.active
						? "Append the valid waypoints to your list"
						: "Nothing importable in the current preview")));
	}

	private void importWaypoints() {
		WaypointSharing.ImportPlan current = this.plan;

		if (current == null || !current.canImport()) {
			return;
		}

		List<Waypoint> added = current.waypointsToAdd();
		CoordKitConfig config = CoordKitConfig.get();
		config.waypoints.addAll(added);
		config.save();

		int duplicates = current.duplicateCount();
		int invalid = current.invalidCount();
		StringBuilder action = new StringBuilder("Imported ").append(added.size());

		if (duplicates > 0) {
			action.append(", skipped ").append(duplicates);
		}

		ClientFeedback.actionBar(this.minecraft, action.toString());

		StringBuilder details = new StringBuilder("CoordKit imported ")
				.append(added.size())
				.append(added.size() == 1 ? " waypoint" : " waypoints");

		if (duplicates > 0) {
			details.append(", skipped ").append(duplicates)
					.append(duplicates == 1 ? " duplicate" : " duplicates");
		}

		if (invalid > 0) {
			details.append(", ignored ").append(invalid)
					.append(invalid == 1 ? " invalid entry" : " invalid entries");
		}

		ClientFeedback.chat(this.minecraft, details.append('.').toString());
		this.minecraft.gui.setScreen(this.parent);
	}

	private String currentWorldKey() {
		return WaypointScope.currentWorldKey(this.minecraft);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

		int width = Math.min(300, this.width - 20);
		int left = Math.max(10, (this.width - width) / 2);
		graphics.text(this.font, "Source: " + this.sourceLabel, left, 24, MUTED_COLOR, false);

		int y = 44;

		if (this.readError != null) {
			drawWrapped(graphics, this.readError, left, y, width, ERROR_COLOR);
		} else if (this.plan == null) {
			graphics.text(this.font, "Nothing to preview.", left, y, MUTED_COLOR, false);
			return;
		} else if (this.plan.error() != null) {
			y = drawWrapped(graphics, this.plan.error(), left, y, width, ERROR_COLOR) + 6;

			if (y < this.height - 90) {
				graphics.text(this.font,
						"Copy a CK1 code or JSON, then press Refresh.",
						left, y, MUTED_COLOR, false);
			}

			return;
		} else {
			int valid = this.plan.validCount();
			int duplicates = this.plan.duplicateCount();
			int invalid = this.plan.invalidCount();
			int total = valid + duplicates + invalid;

			graphics.text(this.font, "Entries: " + total, left, y, TEXT_COLOR, false);
			y += 14;
			graphics.text(this.font,
					"Valid: " + valid + "   Duplicates: " + duplicates + "   Invalid: " + invalid,
					left, y, TEXT_COLOR, false);
			y += 16;

			if (valid == 0) {
				graphics.text(this.font, "Nothing new to import.", left, y, MUTED_COLOR, false);
				y += 14;
			}

			if (invalid > 0) {
				graphics.text(this.font,
						this.plan.allOrNothing()
								? "JSON is all-or-nothing: nothing is added."
								: "Valid share codes can still be imported.",
						left, y, WARNING_COLOR, false);
				y += 16;
			}

			if (!this.plan.invalid().isEmpty()) {
				graphics.text(this.font, "First problems:", left, y, MUTED_COLOR, false);
				y += 14;

				for (int i = 0; i < Math.min(3, this.plan.invalid().size()); i++) {
					WaypointSharing.InvalidEntry entry = this.plan.invalid().get(i);
					String line = "  " + entry.location() + ": " + entry.reason();
					graphics.text(this.font, this.font.plainSubstrByWidth(line, width), left, y, ERROR_COLOR, false);
					y += 12;
				}
			}
		}
	}

	private int drawWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);

		for (FormattedCharSequence line : lines) {
			if (y > this.height - 96) {
				break;
			}

			graphics.text(this.font, line, x, y, color, false);
			y += 12;
		}

		return y;
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
}
