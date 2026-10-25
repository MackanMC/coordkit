package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.ClientFeedback;
import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointScope;
import com.mackan.coordkit.client.WaypointStyle;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
	private boolean scoped;
	private String scopeKey;
	private String error = "";

	public WaypointEditScreen(WaypointManagerScreen parent, Waypoint waypoint, boolean isNew) {
		super(Component.literal(isNew ? "New Waypoint" : "Edit Waypoint"));
		this.parent = parent;
		this.waypoint = waypoint;
		this.isNew = isNew;
		this.colorIndex = WaypointStyle.indexOf(waypoint.color);
		this.scopeKey = waypoint.world == null ? "" : waypoint.world;
		this.scoped = !this.scopeKey.isEmpty();
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
		}).bounds(left, 116, 140, 20).build();
		addRenderableWidget(this.colorButton);

		Button scopeButton = Button.builder(scopeLabel(), button -> toggleScope(button))
				.bounds(left + 160, 116, 140, 20).build();
		scopeButton.setTooltip(Tooltip.create(Component.literal(
				"Only show this waypoint in one world or server. All shows it in every world.")));
		addRenderableWidget(scopeButton);

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

	private Component scopeLabel() {
		if (!this.scoped) {
			return Component.literal("World: All");
		}

		String current = WaypointScope.currentWorldKey(this.minecraft);
		return Component.literal(current.equals(this.scopeKey) ? "World: This" : "World: Other");
	}

	private void toggleScope(Button button) {
		if (this.scoped) {
			this.scoped = false;
		} else {
			String current = WaypointScope.currentWorldKey(this.minecraft);

			if (current.isEmpty()) {
				this.error = "World is not available here";
				return;
			}

			this.scopeKey = current;
			this.scoped = true;
		}

		this.error = "";
		button.setMessage(scopeLabel());
	}

	private void save() {
		Double x = parseValue(this.xBox.getValue());
		Double y = parseValue(this.yBox.getValue());
		Double z = parseValue(this.zBox.getValue());

		if (x == null || y == null || z == null) {
			this.error = "Coordinates must be valid numbers";
			return;
		}

		this.error = "";
		String name = this.nameBox.getValue().trim();
		this.waypoint.name = name.isEmpty() ? "Waypoint" : name;
		this.waypoint.x = x;
		this.waypoint.y = y;
		this.waypoint.z = z;
		this.waypoint.color = WaypointStyle.colorAt(this.colorIndex);
		this.waypoint.world = this.scoped ? this.scopeKey : "";

		// Editing an automatic death waypoint turns it into a normal waypoint,
		// so the next death does not overwrite the player's changes.
		this.waypoint.autoDeath = false;

		CoordKitConfig config = CoordKitConfig.get();

		if (this.isNew) {
			config.waypoints.add(this.waypoint);
		}

		config.save();
		ClientFeedback.actionBar(this.minecraft, "Waypoint saved");
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
		ClientFeedback.actionBar(this.minecraft, "Waypoint deleted");
		back();
	}

	private void copyTeleport() {
		Double x = parseValue(this.xBox.getValue());
		Double y = parseValue(this.yBox.getValue());
		Double z = parseValue(this.zBox.getValue());

		if (x == null || y == null || z == null) {
			this.error = "Coordinates must be valid numbers";
			return;
		}

		this.error = "";
		String command = String.format(Locale.ROOT, "/tp @s %.2f %.2f %.2f", x, y, z);
		this.minecraft.keyboardHandler.setClipboard(command);
		ClientFeedback.chat(this.minecraft, "Copied: " + command);
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
		graphics.text(this.font, "Dimension: " + this.waypoint.dimensionShortName(),
				left, 140, 0xFFA0A0A0, false);

		int swatchColor = 0xFF000000 | WaypointStyle.colorAt(this.colorIndex);
		graphics.fill(left + 144, 120, left + 156, 132, swatchColor);

		Component footer = this.error.isEmpty()
				? Component.literal("Enter saves  ·  Esc cancels")
				: Component.literal(this.error);
		graphics.centeredText(this.font, footer, this.width / 2, this.height - 26,
				this.error.isEmpty() ? 0xFF808080 : 0xFFFF5555);
	}

	@Override
	public void onClose() {
		back();
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static Double parseValue(String raw) {
		try {
			double value = Double.parseDouble(raw.trim().replace(',', '.'));
			return Double.isFinite(value) ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
