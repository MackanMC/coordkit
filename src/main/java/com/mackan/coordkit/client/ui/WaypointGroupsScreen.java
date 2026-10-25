package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.WaypointGroups;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the flat waypoint groups and lets the player rename or delete them.
 * Deleting a group only clears the label; waypoints are never removed.
 */
public final class WaypointGroupsScreen extends Screen {
	private static final int TOP = 34;
	private static final int ROW_HEIGHT = 26;
	private static final int GAP = 4;

	private final WaypointManagerScreen parent;
	private final List<AbstractWidget> rowWidgets = new ArrayList<>();
	private List<String> groups = List.of();
	private int page;
	private String armedDelete = "";

	public WaypointGroupsScreen(WaypointManagerScreen parent) {
		super(Component.literal("Waypoint Groups"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.rowWidgets.clear();

		addRenderableWidget(Button.builder(
				Component.literal("Back"),
				button -> back()
		).bounds(this.width / 2 - 150, this.height - 28, 300, 20).build());

		rebuildRows();
	}

	private void rebuildRows() {
		for (AbstractWidget widget : this.rowWidgets) {
			removeWidget(widget);
		}

		this.rowWidgets.clear();
		this.groups = WaypointGroups.all(CoordKitConfig.get().waypoints);

		if (!this.armedDelete.isEmpty()
				&& this.groups.stream().noneMatch(group -> group.equalsIgnoreCase(this.armedDelete))) {
			this.armedDelete = "";
		}

		int rowWidth = Math.min(300, this.width - 20);
		int left = (this.width - rowWidth) / 2;
		int nameWidth = rowWidth - 58 - 62 - GAP * 2;
		int rowsPerPage = rowsPerPage();
		int pages = Math.max(1, (this.groups.size() + rowsPerPage - 1) / rowsPerPage);
		this.page = Math.max(0, Math.min(this.page, pages - 1));
		int first = this.page * rowsPerPage;
		int last = Math.min(this.groups.size(), first + rowsPerPage);

		for (int i = first; i < last; i++) {
			String group = this.groups.get(i);
			int count = WaypointGroups.count(CoordKitConfig.get().waypoints, group);
			int y = TOP + (i - first) * ROW_HEIGHT;

			Button label = Button.builder(
					Component.literal(clip(group + "  ·  " + count, nameWidth - 8)),
					button -> {
					}
			).bounds(left, y, nameWidth, 20).build();
			label.active = false;
			this.rowWidgets.add(addRenderableWidget(label));

			Button rename = Button.builder(
					Component.literal("Rename"),
					button -> this.minecraft.gui.setScreen(new GroupRenameScreen(this, group))
			).bounds(left + nameWidth + GAP, y, 58, 20).build();
			rename.setTooltip(Tooltip.create(Component.literal("Rename this group on all its waypoints")));
			this.rowWidgets.add(addRenderableWidget(rename));

			boolean armed = group.equalsIgnoreCase(this.armedDelete);
			Button delete = Button.builder(
					Component.literal(armed ? "Sure?" : "Delete"),
					button -> deleteOrArm(group, count)
			).bounds(left + nameWidth + GAP + 58 + GAP, y, 62, 20).build();
			delete.setTooltip(Tooltip.create(Component.literal(armed
					? "Click again to ungroup these " + count + " waypoints"
					: "Clear this group from " + count + " waypoints. The waypoints stay.")));
			this.rowWidgets.add(addRenderableWidget(delete));
		}
	}

	private void deleteOrArm(String group, int count) {
		if (!group.equalsIgnoreCase(this.armedDelete)) {
			this.armedDelete = group;
			ClientFeedback.actionBar(this.minecraft,
					"Press again to ungroup " + count + (count == 1 ? " waypoint" : " waypoints"));
			rebuildRows();
			return;
		}

		int changed = WaypointGroups.clear(CoordKitConfig.get().waypoints, group);
		CoordKitConfig.get().save();
		this.armedDelete = "";
		ClientFeedback.actionBar(this.minecraft,
				"Group deleted, " + changed + (changed == 1 ? " waypoint kept" : " waypoints kept"));
		rebuildRows();
	}

	private int rowsPerPage() {
		return Math.max(1, (this.height - TOP - 46) / ROW_HEIGHT);
	}

	private int pageCount() {
		return Math.max(1, (this.groups.size() + rowsPerPage() - 1) / rowsPerPage());
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
		this.page = Math.max(0, Math.min(this.page, pageCount() - 1));

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

		if (this.groups.isEmpty()) {
			graphics.centeredText(this.font,
					Component.literal("No groups yet."),
					this.width / 2, this.height / 2 - 18, 0xFFA0A0A0);
			graphics.centeredText(this.font,
					Component.literal("Set a group in a waypoint's editor."),
					this.width / 2, this.height / 2 - 4, 0xFFA0A0A0);
		} else {
			graphics.centeredText(this.font,
					Component.literal("Deleting a group keeps its waypoints."),
					this.width / 2, 20, 0xFFA0A0A0);
		}

		int pages = pageCount();

		if (pages > 1) {
			graphics.centeredText(this.font,
					Component.literal("Page " + (this.page + 1) + " / " + pages + "  ·  scroll to browse"),
					this.width / 2, this.height - 42, 0xFFA0A0A0);
		}
	}

	private void back() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private String clip(String text, int width) {
		if (this.font.width(text) <= width) {
			return text;
		}

		String ellipsis = "...";
		return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width(ellipsis))) + ellipsis;
	}

	@Override
	public void onClose() {
		back();
	}
}
