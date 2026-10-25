package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointGroups;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Renames one flat group across every waypoint that belongs to it.
 */
public final class GroupRenameScreen extends Screen {
	private final WaypointGroupsScreen parent;
	private final String oldGroup;
	private EditBox nameBox;
	private String error = "";

	public GroupRenameScreen(WaypointGroupsScreen parent, String group) {
		super(Component.literal("Rename Group"));
		this.parent = parent;
		this.oldGroup = group;
	}

	@Override
	protected void init() {
		super.init();

		int left = this.width / 2 - 150;

		this.nameBox = new EditBox(this.font, left, 52, 300, 20, Component.literal("Group name"));
		this.nameBox.setMaxLength(Waypoint.MAX_GROUP_LENGTH);
		this.nameBox.setValue(this.oldGroup);
		addRenderableWidget(this.nameBox);
		setInitialFocus(this.nameBox);
		this.nameBox.moveCursorToEnd(false);

		addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
				.bounds(left, 86, 148, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> back())
				.bounds(left + 152, 86, 148, 20).build());
	}

	private void save() {
		String target = Waypoint.normalizeGroup(this.nameBox.getValue());

		if (target.isEmpty()) {
			this.error = "Enter a group name";
			return;
		}

		int changed = WaypointGroups.rename(CoordKitConfig.get().waypoints, this.oldGroup, target);

		if (changed == 0) {
			ClientFeedback.actionBar(this.minecraft, "No waypoints in that group");
			back();
			return;
		}

		CoordKitConfig.get().save();
		ClientFeedback.actionBar(this.minecraft,
				"Renamed " + changed + (changed == 1 ? " waypoint" : " waypoints") + " to " + target);
		back();
	}

	private void back() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RETURN) {
			save();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
		graphics.centeredText(this.font,
				Component.literal(this.font.plainSubstrByWidth(
						"Renaming " + this.oldGroup + " on every waypoint.", this.width - 20)),
				this.width / 2, 36, 0xFFA0A0A0);
		graphics.centeredText(this.font,
				Component.literal(this.error.isEmpty() ? "Enter saves  ·  Esc cancels" : this.error),
				this.width / 2, this.height - 16,
				this.error.isEmpty() ? 0xFF808080 : 0xFFFF5555);
	}

	@Override
	public void onClose() {
		back();
	}
}
