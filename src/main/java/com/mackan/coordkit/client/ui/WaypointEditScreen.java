package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointStyle;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Create or edit a single waypoint: name, coordinates, colour.
 */
public final class WaypointEditScreen extends Screen {
	private final WaypointManagerScreen parent;
	private final Waypoint waypoint;
	private final boolean isNew;

	private EditBox nameBox;
	private EditBox xBox;
	private EditBox yBox;
	private EditBox zBox;
	private Button colorButton;
	private Button deleteButton;

	private int colorIndex;
	private boolean deleteArmed;

	public WaypointEditScreen(WaypointManagerScreen parent, Waypoint waypoint, boolean isNew) {
		super(Component.literal(isNew ? "New Waypoint" : "Edit Waypoint"));
		this.parent = parent;
		this.waypoint = waypoint;
		this.isNew = isNew;
		this.colorIndex = WaypointStyle.indexOf(waypoint.color);
	}

	@Override
	protected void init() {
		super.init();

		int left = this.width / 2 - 150;

		this.nameBox = new EditBox(this.font, left + 46, 46, 254, 20, Component.literal("Name"));
		this.nameBox.setMaxLength(32);
		this.nameBox.setValue(this.waypoint.name);
		addRenderableWidget(this.nameBox);
		setInitialFocus(this.nameBox);

		this.xBox = new EditBox(this.font, left, 84, 92, 20, Component.literal("X"));
		this.xBox.setMaxLength(20);
		this.xBox.setValue(format(this.waypoint.x));
		this.xBox.setHint(Component.literal("X"));
		addRenderableWidget(this.xBox);

		this.yBox = new EditBox(this.font, left + 104, 84, 92, 20, Component.literal("Y"));
		this.yBox.setMaxLength(20);
		this.yBox.setValue(format(this.waypoint.y));
		this.yBox.setHint(Component.literal("Y"));
		addRenderableWidget(this.yBox);

		this.zBox = new EditBox(this.font, left + 208, 84, 92, 20, Component.literal("Z"));
		this.zBox.setMaxLength(20);
		this.zBox.setValue(format(this.waypoint.z));
		this.zBox.setHint(Component.literal("Z"));
		addRenderableWidget(this.zBox);

		this.colorButton = Button.builder(colorLabel(), button -> {
			this.colorIndex = (this.colorIndex + 1) % WaypointStyle.COLORS.length;
			button.setMessage(colorLabel());
		}).bounds(left, 116, 150, 20).build();
		addRenderableWidget(this.colorButton);

		addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
				.bounds(left, 152, 148, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> back())
				.bounds(left + 152, 152, 148, 20).build());

		if (this.isNew) {
			addRenderableWidget(Button.builder(Component.literal("Copy /tp"), button -> copyTeleport())
					.bounds(left, 180, 300, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.literal("Copy /tp"), button -> copyTeleport())
					.bounds(left, 180, 148, 20).build());

			this.deleteButton = Button.builder(Component.literal("Delete"), button -> delete())
					.bounds(left + 152, 180, 148, 20).build();
			addRenderableWidget(this.deleteButton);
		}
	}

	private Component colorLabel() {
		return Component.literal("Color: " + WaypointStyle.nameAt(this.colorIndex));
	}

	private void save() {
		String name = this.nameBox.getValue().trim();
		this.waypoint.name = name.isEmpty() ? "Waypoint" : name;
		this.waypoint.x = parse(this.xBox.getValue(), this.waypoint.x);
		this.waypoint.y = parse(this.yBox.getValue(), this.waypoint.y);
		this.waypoint.z = parse(this.zBox.getValue(), this.waypoint.z);
		this.waypoint.color = WaypointStyle.colorAt(this.colorIndex);

		CoordKitConfig config = CoordKitConfig.get();

		if (this.isNew) {
			config.waypoints.add(this.waypoint);
		}

		config.save();
		back();
	}

	private void delete() {
		if (this.isNew) {
			return;
		}

		if (!this.deleteArmed) {
			this.deleteArmed = true;
			this.deleteButton.setMessage(Component.literal("Confirm delete"));
			return;
		}

		CoordKitConfig config = CoordKitConfig.get();
		config.waypoints.remove(this.waypoint);
		config.save();
		back();
	}

	private void copyTeleport() {
		String command = String.format(
				Locale.ROOT,
				"/tp @s %.2f %.2f %.2f",
				parse(this.xBox.getValue(), this.waypoint.x),
				parse(this.yBox.getValue(), this.waypoint.y),
				parse(this.zBox.getValue(), this.waypoint.z)
		);

		this.minecraft.keyboardHandler.setClipboard(command);

		if (this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(Component.literal("Copied: " + command));
		}
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

		int left = this.width / 2 - 150;
		graphics.text(this.font, "Name", left, 52, 0xFFA0A0A0, false);
		graphics.text(this.font, "Position (X / Y / Z)", left, 72, 0xFFA0A0A0, false);
		graphics.text(this.font, this.waypoint.dimensionShortName(), left + 174, 122, 0xFFA0A0A0, false);

		int swatchColor = 0xFF000000 | WaypointStyle.colorAt(this.colorIndex);
		graphics.fill(left + 156, 120, left + 168, 132, swatchColor);

		graphics.centeredText(this.font, Component.literal("Enter saves  ·  Esc cancels"),
				this.width / 2, this.height - 26, 0xFF808080);
	}

	@Override
	public void onClose() {
		back();
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static double parse(String raw, double fallback) {
		try {
			return Double.parseDouble(raw.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
