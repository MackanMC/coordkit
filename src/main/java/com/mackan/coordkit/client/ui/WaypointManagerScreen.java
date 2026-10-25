package com.mackan.coordkit.client.ui;

import com.mackan.coordkit.client.CoordKitConfig;
import com.mackan.coordkit.client.Waypoint;
import com.mackan.coordkit.client.WaypointStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Scrollable list of saved waypoints with add / edit / copy / delete actions.
 */
public final class WaypointManagerScreen extends Screen {
	private static final int ROW_HEIGHT = 28;
	private static final int TOP = 54;

	private final Screen parent;
	private int page;
	private Waypoint pendingDelete;

	public WaypointManagerScreen(Screen parent) {
		super(Component.literal("Waypoints"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		refresh();
	}

	/**
	 * Rebuilds every widget; called after any change and after returning from
	 * the editor.
	 */
	public void refresh() {
		this.clearWidgets();

		List<Waypoint> waypoints = CoordKitConfig.get().waypoints;
		int rowsPerPage = rowsPerPage();
		int pages = pageCount();
		this.page = Math.max(0, Math.min(this.page, pages - 1));

		int rowWidth = Math.min(360, this.width - 20);
		int left = (this.width - rowWidth) / 2;
		int toggleWidth = 34;
		int copyWidth = 46;
		int deleteWidth = 36;
		int gap = 4;
		int nameWidth = rowWidth - toggleWidth - copyWidth - deleteWidth - gap * 3;
		int first = this.page * rowsPerPage;
		int last = Math.min(waypoints.size(), first + rowsPerPage);

		for (int i = first; i < last; i++) {
			Waypoint waypoint = waypoints.get(i);
			int y = TOP + (i - first) * ROW_HEIGHT;
			int x = left;

			addRenderableWidget(Button.builder(
					Component.literal(waypoint.visible ? "ON" : "OFF"),
					button -> {
						waypoint.visible = !waypoint.visible;
						CoordKitConfig.get().save();
						refresh();
					}
			).bounds(x, y, toggleWidth, 20).build());

			x += toggleWidth + gap;
			addRenderableWidget(Button.builder(
					Component.literal(rowLabel(waypoint)),
					button -> this.minecraft.gui.setScreen(new WaypointEditScreen(this, waypoint, false))
			).bounds(x, y, nameWidth, 20).build());

			x += nameWidth + gap;
			addRenderableWidget(Button.builder(
					Component.literal("TP"),
					button -> copyTeleport(waypoint)
			).bounds(x, y, copyWidth, 20).build());

			x += copyWidth + gap;
			addRenderableWidget(Button.builder(
					Component.literal(this.pendingDelete == waypoint ? "Sure?" : "Del"),
					button -> deleteOrArm(waypoint)
			).bounds(x, y, deleteWidth, 20).build());
		}

		int bottom = this.height - 28;
		int buttonWidth = 100;
		int totalWidth = buttonWidth * 3 + gap * 2;
		int buttonX = (this.width - totalWidth) / 2;

		Button add = Button.builder(
				Component.literal("Add here"),
				button -> addHere()
		).bounds(buttonX, bottom, buttonWidth, 20).build();
		add.active = this.minecraft.player != null && this.minecraft.level != null;
		addRenderableWidget(add);

		addRenderableWidget(Button.builder(
				Component.literal("Settings"),
				button -> this.minecraft.gui.setScreen(new CoordKitConfigScreen(this))
		).bounds(buttonX + buttonWidth + gap, bottom, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(
				Component.literal("Done"),
				button -> this.onClose()
		).bounds(buttonX + (buttonWidth + gap) * 2, bottom, buttonWidth, 20).build());
	}

	private String rowLabel(Waypoint waypoint) {
		if (this.minecraft.player == null || this.minecraft.level == null) {
			return waypoint.name;
		}

		boolean sameDimension = waypoint.dimensionId().equals(this.minecraft.level.dimension().identifier());

		if (!sameDimension) {
			return waypoint.name + "  ·  " + waypoint.dimensionShortName();
		}

		double distance = waypoint.distanceTo(
				this.minecraft.player.getX(),
				this.minecraft.player.getY(),
				this.minecraft.player.getZ()
		);

		return waypoint.name + "  ·  " + formatDistance(distance);
	}

	private int rowsPerPage() {
		return Math.max(1, (this.height - TOP - 46) / ROW_HEIGHT);
	}

	private int pageCount() {
		int size = CoordKitConfig.get().waypoints.size();
		return Math.max(1, (size + rowsPerPage() - 1) / rowsPerPage());
	}

	private void addHere() {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.level == null) {
			return;
		}

		CoordKitConfig config = CoordKitConfig.get();
		String dimension = client.level.dimension().identifier().toString();
		Waypoint waypoint = new Waypoint(
				"Waypoint " + (config.waypoints.size() + 1),
				client.player.getX(),
				client.player.getY(),
				client.player.getZ(),
				dimension,
				WaypointStyle.colorAt(config.waypoints.size())
		);

		client.gui.setScreen(new WaypointEditScreen(this, waypoint, true));
	}

	private void copyTeleport(Waypoint waypoint) {
		this.minecraft.keyboardHandler.setClipboard(waypoint.command());

		if (this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(Component.literal("Copied: " + waypoint.command()));
		}
	}

	private void deleteOrArm(Waypoint waypoint) {
		if (this.pendingDelete == waypoint) {
			CoordKitConfig.get().waypoints.remove(waypoint);
			CoordKitConfig.get().save();
			this.pendingDelete = null;
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
			refresh();
			return true;
		}

		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);

		int count = CoordKitConfig.get().waypoints.size();

		if (count == 0) {
			graphics.centeredText(this.font,
					Component.literal("No waypoints yet - press \"Add here\" to save your position."),
					this.width / 2, this.height / 2 - 10, 0xFFA0A0A0);
		} else {
			graphics.centeredText(this.font,
					Component.literal(count + (count == 1 ? " waypoint" : " waypoints")
							+ "  ·  scroll to browse  ·  Del twice to confirm"),
					this.width / 2, 32, 0xFFA0A0A0);
		}

		int pages = pageCount();

		if (pages > 1) {
			graphics.centeredText(this.font,
					Component.literal("Page " + (this.page + 1) + " / " + pages),
					this.width / 2, this.height - 42, 0xFFA0A0A0);
		}
	}

	private static String formatDistance(double distance) {
		if (distance < 1000.0) {
			return Math.round(distance) + "m";
		}

		return String.format(java.util.Locale.ROOT, "%.1fkm", distance / 1000.0);
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
