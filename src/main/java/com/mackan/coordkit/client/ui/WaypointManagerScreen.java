package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointActions;
import com.mackan.coordkit.client.WaypointQuery;
import com.mackan.coordkit.client.WaypointScope;
import com.mackan.coordkit.client.WaypointSharing;
import com.mackan.coordkit.client.WaypointStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Scrollable list of saved waypoints with search, filters, sorting and
 * add / edit / copy / delete actions.
 */
public final class WaypointManagerScreen extends Screen {
	private static final int ROW_HEIGHT = 28;
	private static final int TOP = 78;
	private static final int GAP = 4;

	private final Screen parent;
	private final List<AbstractWidget> rowWidgets = new ArrayList<>();
	private List<Waypoint> filtered = List.of();
	private int page;
	private Waypoint pendingDelete;

	private EditBox searchBox;
	private Button scopeButton;
	private Button groupButton;
	private Button sortButton;

	private String query = "";
	private WaypointQuery.Scope scopeFilter = WaypointQuery.Scope.VISIBLE_HERE;
	private WaypointQuery.GroupFilter groupFilter = WaypointQuery.GroupFilter.ALL;
	private String groupFilterValue = "";
	private WaypointQuery.Sort sortMode = WaypointQuery.Sort.NAME;

	public WaypointManagerScreen(Screen parent) {
		super(Component.literal("Waypoints"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.rowWidgets.clear();
		buildControls();
		refresh();
	}

	/**
	 * Rebuilds the row widgets after a waypoint changed; called after any
	 * change and after returning from the editor.
	 */
	public void refresh() {
		ensureGroupFilterValid();
		updateControlLabels();
		rebuildRows();
	}

	/**
	 * Creates the search box, filters and the bottom button bar. These widgets
	 * survive row rebuilds so typing never loses focus or the caret.
	 */
	private void buildControls() {
		int rowWidth = Math.min(360, this.width - 20);
		int left = (this.width - rowWidth) / 2;

		this.searchBox = new EditBox(this.font, left, 30, rowWidth, 20, Component.literal("Search"));
		this.searchBox.setHint(Component.literal("Search name or group"));
		this.searchBox.setMaxLength(64);

		// Set the retained text before installing the responder so reopening
		// or resizing the screen does not reset the page.
		if (!this.query.isEmpty()) {
			this.searchBox.setValue(this.query);
		}

		this.searchBox.setResponder(value -> {
			String next = value == null ? "" : value;

			if (next.equals(this.query)) {
				return;
			}

			this.query = next;
			resetPage();
			rebuildRows();
		});
		addRenderableWidget(this.searchBox);

		int filterWidth = Math.max(50, (rowWidth - GAP * 2) / 3);
		int filtersWidth = filterWidth * 3 + GAP * 2;
		int filterX = left + (rowWidth - filtersWidth) / 2;
		int filterY = 52;

		this.scopeButton = addRenderableWidget(Button.builder(scopeLabel(), button -> {
			this.scopeFilter = this.scopeFilter == WaypointQuery.Scope.VISIBLE_HERE
					? WaypointQuery.Scope.ALL_WORLDS
					: WaypointQuery.Scope.VISIBLE_HERE;
			filterChanged();
		}).bounds(filterX, filterY, filterWidth, 20).build());

		filterX += filterWidth + GAP;
		this.groupButton = addRenderableWidget(Button.builder(groupLabel(), button -> {
			cycleGroupFilter();
			filterChanged();
		}).bounds(filterX, filterY, filterWidth, 20).build());

		filterX += filterWidth + GAP;
		this.sortButton = addRenderableWidget(Button.builder(sortLabel(), button -> {
			this.sortMode = switch (this.sortMode) {
				case NAME -> WaypointQuery.Sort.GROUP;
				case GROUP -> WaypointQuery.Sort.DISTANCE;
				case DISTANCE -> WaypointQuery.Sort.NAME;
			};
			filterChanged();
		}).bounds(filterX, filterY, filterWidth, 20).build());

		updateControlLabels();

		int bottom = this.height - 28;
		int available = Math.max(240, this.width - 20);
		int buttonWidth = Math.min(100, (available - GAP * 3) / 4);
		int totalWidth = buttonWidth * 4 + GAP * 3;
		int buttonX = (this.width - totalWidth) / 2;

		int secondaryWidth = Math.min(100, (available - GAP) / 2);
		int secondaryTotal = secondaryWidth * 2 + GAP;
		int secondaryX = (this.width - secondaryTotal) / 2;
		int secondaryY = bottom - 24;

		addRenderableWidget(Button.builder(
				Component.literal("Export..."),
				button -> this.minecraft.gui.setScreen(new WaypointExportScreen(this))
		).bounds(secondaryX, secondaryY, secondaryWidth, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Import..."),
				button -> this.minecraft.gui.setScreen(new WaypointImportScreen(this))
		).bounds(secondaryX + secondaryWidth + GAP, secondaryY, secondaryWidth, 20).build());

		Button add = Button.builder(
				Component.literal("Add here"),
				button -> addHere()
		).bounds(buttonX, bottom, buttonWidth, 20).build();
		add.active = this.minecraft.player != null && this.minecraft.level != null;
		add.setTooltip(Tooltip.create(Component.literal("Save your current position")));
		addRenderableWidget(add);

		Button addAim = Button.builder(
				Component.literal("Add aim"),
				button -> addLookingAt()
		).bounds(buttonX + buttonWidth + GAP, bottom, buttonWidth, 20).build();
		addAim.active = this.minecraft.player != null && this.minecraft.level != null;
		addAim.setTooltip(Tooltip.create(Component.literal("Save the block you are looking at, up to 128 blocks away")));
		addRenderableWidget(addAim);

		addRenderableWidget(Button.builder(
				Component.literal("Settings"),
				button -> this.minecraft.gui.setScreen(new CoordKitConfigScreen(this))
		).bounds(buttonX + (buttonWidth + GAP) * 2, bottom, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Done"),
				button -> this.onClose()
		).bounds(buttonX + (buttonWidth + GAP) * 3, bottom, buttonWidth, 20).build());
	}

	private void rebuildRows() {
		for (AbstractWidget widget : this.rowWidgets) {
			removeWidget(widget);
		}

		this.rowWidgets.clear();
		this.filtered = filteredWaypoints();

		int rowsPerPage = rowsPerPage();
		int pages = Math.max(1, (this.filtered.size() + rowsPerPage - 1) / rowsPerPage);
		this.page = Math.max(0, Math.min(this.page, pages - 1));

		int rowWidth = Math.min(360, this.width - 20);
		int left = (this.width - rowWidth) / 2;
		int toggleWidth = 30;
		int copyWidth = 34;
		int shareWidth = 44;
		int deleteWidth = 36;
		int nameWidth = rowWidth - toggleWidth - copyWidth - shareWidth - deleteWidth - GAP * 4;
		int first = this.page * rowsPerPage;
		int last = Math.min(this.filtered.size(), first + rowsPerPage);

		for (int i = first; i < last; i++) {
			Waypoint waypoint = this.filtered.get(i);
			int y = TOP + (i - first) * ROW_HEIGHT;
			int x = left;

			Button toggle = addRenderableWidget(Button.builder(
					Component.literal(waypoint.visible ? "ON" : "OFF"),
					button -> {
						waypoint.visible = !waypoint.visible;
						CoordKitConfig.get().save();
						ClientFeedback.actionBar(this.minecraft,
								waypoint.name + (waypoint.visible ? " shown" : " hidden"));
						refresh();
					}
			).bounds(x, y, toggleWidth, 20).build());
			this.rowWidgets.add(toggle);

			x += toggleWidth + GAP;
			Button name = addRenderableWidget(Button.builder(
					Component.literal(rowLabel(waypoint)),
					button -> this.minecraft.gui.setScreen(new WaypointEditScreen(this, waypoint, false))
			).bounds(x, y, nameWidth, 20).build());
			name.setTooltip(Tooltip.create(Component.literal(waypointTooltip(waypoint))));
			this.rowWidgets.add(name);

			x += nameWidth + GAP;
			Button copy = addRenderableWidget(Button.builder(
					Component.literal("TP"),
					button -> copyTeleport(waypoint)
			).bounds(x, y, copyWidth, 20).build());
			copy.setTooltip(Tooltip.create(Component.literal("Copy a teleport command")));
			this.rowWidgets.add(copy);

			x += copyWidth + GAP;
			Button share = addRenderableWidget(Button.builder(
					Component.literal("Share"),
					button -> shareWaypoint(waypoint)
			).bounds(x, y, shareWidth, 20).build());
			share.setTooltip(Tooltip.create(Component.literal("Copy a CK1 share code")));
			this.rowWidgets.add(share);

			x += shareWidth + GAP;
			Button delete = addRenderableWidget(Button.builder(
					Component.literal(this.pendingDelete == waypoint ? "Sure?" : "Del"),
					button -> deleteOrArm(waypoint)
			).bounds(x, y, deleteWidth, 20).build());
			delete.setTooltip(Tooltip.create(Component.literal("Click once to arm, click again to delete")));
			this.rowWidgets.add(delete);
		}
	}

	/**
	 * Applies search, scope and group filters, then the chosen sort. The
	 * returned list is a copy, so paging never changes the config order.
	 */
	private List<Waypoint> filteredWaypoints() {
		WaypointQuery.Position position = null;

		if (this.minecraft.player != null && this.minecraft.level != null) {
			position = new WaypointQuery.Position(
					this.minecraft.player.getX(),
					this.minecraft.player.getY(),
					this.minecraft.player.getZ()
			);
		}

		return WaypointQuery.filter(
				CoordKitConfig.get().waypoints,
				this.query,
				this.scopeFilter,
				WaypointScope.currentWorldKey(this.minecraft),
				this.groupFilter,
				this.groupFilterValue,
				this.sortMode,
				position
		);
	}

	private void filterChanged() {
		resetPage();
		ensureGroupFilterValid();
		updateControlLabels();
		rebuildRows();
	}

	private void resetPage() {
		this.page = 0;
		this.pendingDelete = null;
	}

	private void cycleGroupFilter() {
		List<String> groups = availableGroups();

		switch (this.groupFilter) {
			case ALL -> {
				if (groups.isEmpty()) {
					this.groupFilter = WaypointQuery.GroupFilter.UNGROUPED;
				} else {
					this.groupFilter = WaypointQuery.GroupFilter.SPECIFIC;
					this.groupFilterValue = groups.get(0);
				}
			}
			case UNGROUPED -> {
				if (groups.isEmpty()) {
					this.groupFilter = WaypointQuery.GroupFilter.ALL;
					this.groupFilterValue = "";
				} else {
					this.groupFilter = WaypointQuery.GroupFilter.SPECIFIC;
					this.groupFilterValue = groups.get(0);
				}
			}
			case SPECIFIC -> {
				int index = indexOfGroup(groups, this.groupFilterValue);

				if (index >= 0 && index + 1 < groups.size()) {
					this.groupFilterValue = groups.get(index + 1);
				} else {
					this.groupFilter = WaypointQuery.GroupFilter.ALL;
					this.groupFilterValue = "";
				}
			}
		}
	}

	private void ensureGroupFilterValid() {
		if (this.groupFilter == WaypointQuery.GroupFilter.SPECIFIC
				&& indexOfGroup(availableGroups(), this.groupFilterValue) < 0) {
			this.groupFilter = WaypointQuery.GroupFilter.ALL;
			this.groupFilterValue = "";
		}
	}

	private List<String> availableGroups() {
		Map<String, String> groups = new TreeMap<>();

		for (Waypoint waypoint : CoordKitConfig.get().waypoints) {
			if (waypoint == null) {
				continue;
			}

			String group = Waypoint.normalizeGroup(waypoint.group);

			if (!group.isEmpty()) {
				groups.putIfAbsent(lower(group), group);
			}
		}

		return new ArrayList<>(groups.values());
	}

	private static int indexOfGroup(List<String> groups, String group) {
		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).equalsIgnoreCase(group)) {
				return i;
			}
		}

		return -1;
	}

	private void updateControlLabels() {
		if (this.scopeButton == null || this.groupButton == null || this.sortButton == null) {
			return;
		}

		this.scopeButton.setMessage(scopeLabel());
		this.scopeButton.setTooltip(Tooltip.create(Component.literal(
				this.scopeFilter == WaypointQuery.Scope.VISIBLE_HERE
						? "Showing waypoints for this world and global waypoints"
						: "Showing waypoints from every world")));

		this.groupButton.setMessage(groupLabel());
		this.groupButton.setTooltip(Tooltip.create(Component.literal(
				this.groupFilter == WaypointQuery.GroupFilter.SPECIFIC
						? "Group: " + this.groupFilterValue
						: "Click to cycle group filters")));

		this.sortButton.setMessage(sortLabel());
		this.sortButton.setTooltip(Tooltip.create(Component.literal("Click to change the sort order")));
	}

	private Component scopeLabel() {
		return Component.literal(this.scopeFilter == WaypointQuery.Scope.VISIBLE_HERE ? "Visible here" : "All worlds");
	}

	private Component groupLabel() {
		return switch (this.groupFilter) {
			case ALL -> Component.literal("Group: All");
			case UNGROUPED -> Component.literal("Group: Ungrouped");
			case SPECIFIC -> Component.literal("Group: " + truncate(this.groupFilterValue, 10));
		};
	}

	private Component sortLabel() {
		return switch (this.sortMode) {
			case NAME -> Component.literal("Sort: Name");
			case GROUP -> Component.literal("Sort: Group");
			case DISTANCE -> Component.literal("Sort: Distance");
		};
	}

	private String rowLabel(Waypoint waypoint) {
		String group = Waypoint.normalizeGroup(waypoint.group);
		String label = group.isEmpty() ? waypoint.name : waypoint.name + " [" + group + "]";

		if (this.minecraft.player == null || this.minecraft.level == null) {
			return label;
		}

		if (!WaypointScope.isVisibleIn(waypoint, WaypointScope.currentWorldKey(this.minecraft))) {
			return label + "  ·  other world";
		}

		boolean sameDimension = waypoint.dimensionId().equals(this.minecraft.level.dimension().identifier());

		if (!sameDimension) {
			return label + "  ·  " + waypoint.dimensionShortName();
		}

		double distance = waypoint.distanceTo(
				this.minecraft.player.getX(),
				this.minecraft.player.getY(),
				this.minecraft.player.getZ()
		);

		return label + "  ·  " + formatDistance(distance);
	}

	private String waypointTooltip(Waypoint waypoint) {
		StringBuilder text = new StringBuilder(waypoint.name);
		String group = Waypoint.normalizeGroup(waypoint.group);

		if (!group.isEmpty()) {
			text.append('\n').append("Group: ").append(group);
		}

		text.append('\n')
				.append(String.format(Locale.ROOT, "%.2f, %.2f, %.2f", waypoint.x, waypoint.y, waypoint.z))
				.append('\n')
				.append(waypoint.dimensionShortName());

		return text.toString();
	}

	private int rowsPerPage() {
		return Math.max(1, (this.height - TOP - 70) / ROW_HEIGHT);
	}

	private int pageCount() {
		return Math.max(1, (this.filtered.size() + rowsPerPage() - 1) / rowsPerPage());
	}

	private void addHere() {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.level == null) {
			return;
		}

		CoordKitConfig config = CoordKitConfig.get();
		Waypoint waypoint = WaypointActions.create(
				client,
				config,
				client.player.position(),
				WaypointActions.suggestedName(config),
				WaypointStyle.colorAt(config.waypoints.size())
		);

		client.gui.setScreen(new WaypointEditScreen(this, waypoint, true));
	}

	private void addLookingAt() {
		Minecraft client = Minecraft.getInstance();
		CoordKitConfig config = CoordKitConfig.get();
		Waypoint waypoint = WaypointActions.createAtCrosshair(client, config);

		if (waypoint == null) {
			ClientFeedback.actionBar(client, "Look at a block within 128 blocks");
			return;
		}

		client.gui.setScreen(new WaypointEditScreen(this, waypoint, true));
	}

	private void copyTeleport(Waypoint waypoint) {
		this.minecraft.keyboardHandler.setClipboard(waypoint.command());

		if (this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(Component.literal("Copied: " + waypoint.command()));
		}
	}

	private void shareWaypoint(Waypoint waypoint) {
		String code = WaypointSharing.shareCode(waypoint);

		try {
			this.minecraft.keyboardHandler.setClipboard(code);
		} catch (RuntimeException e) {
			ClientFeedback.chat(this.minecraft, "Could not copy the share code: " + errorText(e));
			return;
		}

		ClientFeedback.actionBar(this.minecraft, "Share code copied");

		if (code.length() > 240) {
			ClientFeedback.chat(this.minecraft, "This share code is " + code.length()
					+ " characters, too long to paste into Minecraft chat in one message."
					+ " Paste it in Discord or a file instead.");
		}
	}

	private static String errorText(Throwable error) {
		String message = error.getMessage();
		return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
	}

	private void deleteOrArm(Waypoint waypoint) {
		if (this.pendingDelete == waypoint) {
			CoordKitConfig.get().waypoints.remove(waypoint);
			CoordKitConfig.get().save();
			this.pendingDelete = null;
			ClientFeedback.actionBar(this.minecraft, "Waypoint deleted");
			refresh();
		} else {
			this.pendingDelete = waypoint;
			refresh();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}

		if (verticalAmount == 0) {
			return false;
		}

		int oldPage = this.page;
		this.page -= (int) Math.signum(verticalAmount);

		if (this.page < 0) {
			this.page = 0;
		}

		int pages = pageCount();

		if (this.page > pages - 1) {
			this.page = pages - 1;
		}

		if (this.page != oldPage) {
			rebuildRows();
			return true;
		}

		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

		int total = CoordKitConfig.get().waypoints.size();
		int shown = this.filtered.size();

		if (total == 0) {
			graphics.centeredText(this.font,
					Component.literal("No waypoints yet."),
					this.width / 2, this.height / 2 - 18, 0xFFA0A0A0);
			graphics.centeredText(this.font,
					Component.literal("Press \"Add here\" to save your position."),
					this.width / 2, this.height / 2 - 4, 0xFFA0A0A0);
		} else if (shown == 0) {
			graphics.centeredText(this.font,
					Component.literal("No waypoints match these filters."),
					this.width / 2, this.height / 2 - 18, 0xFFA0A0A0);
			graphics.centeredText(this.font,
					Component.literal("Adjust the search or cycle the filters."),
					this.width / 2, this.height / 2 - 4, 0xFFA0A0A0);
		} else {
			String count = shown == total
					? total + (total == 1 ? " waypoint" : " waypoints")
					: shown + " of " + total + " waypoints";
			graphics.centeredText(this.font,
					Component.literal(count + "  ·  Del twice to confirm"),
					this.width / 2, 20, 0xFFA0A0A0);
		}

		int pages = pageCount();

		if (pages > 1) {
			graphics.centeredText(this.font,
					Component.literal("Page " + (this.page + 1) + " / " + pages + "  ·  scroll to browse"),
					this.width / 2, this.height - 66, 0xFFA0A0A0);
		}
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private static String truncate(String value, int length) {
		if (value == null || value.length() <= length) {
			return value == null ? "" : value;
		}

		int keep = Math.max(0, length - 3);
		return value.substring(0, keep) + "...";
	}

	private static String formatDistance(double distance) {
		if (distance < 1000.0) {
			return Math.round(distance) + "m";
		}

		return String.format(Locale.ROOT, "%.1fkm", distance / 1000.0);
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
