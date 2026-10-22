package aman037.priority;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ProfilesScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int LIST_WIDTH = 260;
	private static final int MAX_NAME_LENGTH = 24;

	private final ConfigScreen parent;
	private final ConfigHolder<PriorityConfig> holder;
	private final int[] currentValues;
	private final List<Row> rows = new ArrayList<>();

	private EditBox nameField;
	private Button saveButton;
	private Component statusText;
	private int page;
	private int rowsPerPage;
	private int listTop;
	private int panelLeft;
	private int panelRight;
	private int panelTop;
	private int panelBottom;

	public ProfilesScreen(ConfigScreen parent, int[] currentValues) {
		super(Component.literal("Priority Profiles"));
		this.parent = parent;
		this.holder = AutoConfig.getConfigHolder(PriorityConfig.class);
		this.currentValues = currentValues;
	}

	@Override
	protected void init() {
		super.init();
		this.rows.clear();

		int centerX = this.width / 2;
		int left = centerX - LIST_WIDTH / 2;

		this.panelLeft = left - 16;
		this.panelRight = left + LIST_WIDTH + 16;
		this.panelTop = 24;
		this.panelBottom = this.height - 24;

		String oldName = this.nameField != null ? this.nameField.getValue() : "";
		int topY = this.panelTop + 30;
		this.nameField = new EditBox(this.font, left, topY, LIST_WIDTH - 66, 20, Component.literal("Profile name"));
		this.nameField.setMaxLength(MAX_NAME_LENGTH);
		this.nameField.setHint(Component.literal("Profile name").withStyle(ChatFormatting.DARK_GRAY));
		this.nameField.setValue(oldName);
		this.nameField.setResponder(text -> updateSaveButton());
		this.addRenderableWidget(this.nameField);

		this.saveButton = Button.builder(Component.literal("Save"), b -> saveProfile())
			.bounds(left + LIST_WIDTH - 60, topY, 60, 20)
			.build();
		this.addRenderableWidget(this.saveButton);

		this.listTop = topY + 34;
		int listBottom = this.panelBottom - 56;
		this.rowsPerPage = Math.max(1, (listBottom - this.listTop) / ROW_HEIGHT);

		List<String> names = new ArrayList<>(this.holder.getConfig().getProfiles().keySet());
		int pageCount = Math.max(1, (int) Math.ceil(names.size() / (double) this.rowsPerPage));
		this.page = Math.min(this.page, pageCount - 1);

		int start = this.page * this.rowsPerPage;
		int end = Math.min(names.size(), start + this.rowsPerPage);
		for (int i = start; i < end; i++) {
			String name = names.get(i);
			int y = this.listTop + (i - start) * ROW_HEIGHT;

			Button loadButton = Button.builder(Component.literal("Load"), b -> loadProfile(name))
				.bounds(left + LIST_WIDTH - 122, y, 50, 20)
				.build();
			Button upButton = Button.builder(Component.empty(), b -> moveProfile(name, -1))
				.bounds(left + LIST_WIDTH - 68, y, 14, 10)
				.build();
			Button downButton = Button.builder(Component.empty(), b -> moveProfile(name, 1))
				.bounds(left + LIST_WIDTH - 68, y + 10, 14, 10)
				.build();
			upButton.active = i > 0;
			downButton.active = i < names.size() - 1;
			Button deleteButton = Button.builder(Component.literal("Delete").withStyle(ChatFormatting.RED), b -> deleteProfile(name))
				.bounds(left + LIST_WIDTH - 50, y, 50, 20)
				.build();
			this.addRenderableWidget(loadButton);
			this.addRenderableWidget(upButton);
			this.addRenderableWidget(downButton);
			this.addRenderableWidget(deleteButton);

			String shown = name;
			int maxNameWidth = LIST_WIDTH - 128;
			if (this.font.width(shown) > maxNameWidth) {
				shown = this.font.plainSubstrByWidth(shown, maxNameWidth - this.font.width("...")) + "...";
			}
			this.rows.add(new Row(shown, left, y + (20 - this.font.lineHeight) / 2, upButton, downButton));
		}

		int bottomY = this.panelBottom - 28;
		if (pageCount > 1) {
			Button prev = Button.builder(Component.literal("<"), b -> changePage(-1))
				.bounds(left, bottomY - 24, 20, 20)
				.build();
			Button next = Button.builder(Component.literal(">"), b -> changePage(1))
				.bounds(left + LIST_WIDTH - 20, bottomY - 24, 20, 20)
				.build();
			prev.active = this.page > 0;
			next.active = this.page < pageCount - 1;
			this.addRenderableWidget(prev);
			this.addRenderableWidget(next);
		}

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
			.bounds(centerX - 50, bottomY, 100, 20)
			.build());

		updateSaveButton();
	}

	private void updateSaveButton() {
		if (this.saveButton != null) {
			this.saveButton.active = this.currentValues != null && !this.nameField.getValue().isBlank();
		}
	}

	private void saveProfile() {
		if (this.currentValues == null) {
			this.statusText = Component.literal("Fix the priorities first, they arent valid.").withStyle(ChatFormatting.RED);
			return;
		}
		String name = this.nameField.getValue().trim();
		if (name.isEmpty()) {
			return;
		}

		PriorityConfig config = this.holder.getConfig();
		boolean overwrite = config.getProfiles().containsKey(name);
		config.saveProfile(name, this.currentValues);
		this.holder.save();

		this.statusText = Component.literal((overwrite ? "Overwrote " : "Saved ") + "\"" + name + "\"").withStyle(ChatFormatting.GREEN);
		this.nameField.setValue("");
		this.rebuildWidgets();
	}

	private void loadProfile(String name) {
		int[] values = this.holder.getConfig().getProfile(name);
		if (!PriorityConfig.isValidProfile(values)) {
			this.statusText = Component.literal("\"" + name + "\" is broken, check the config file.").withStyle(ChatFormatting.RED);
			return;
		}
		// still have to close the main screen for it to save
		this.parent.loadValues(values);
		Minecraft.getInstance().gui.setScreen(this.parent);
	}

	private void deleteProfile(String name) {
		Minecraft.getInstance().gui.setScreen(new ConfirmScreen(
			confirmed -> {
				if (confirmed) {
					this.holder.getConfig().deleteProfile(name);
					this.holder.save();
					this.statusText = Component.literal("Deleted \"" + name + "\"").withStyle(ChatFormatting.YELLOW);
				}
				Minecraft.getInstance().gui.setScreen(this);
			},
			Component.literal("Delete profile?"),
			Component.literal("\"" + name + "\" will be gone for good.")
		));
	}

	private void moveProfile(String name, int delta) {
		PriorityConfig config = this.holder.getConfig();
		config.moveProfile(name, delta);
		this.holder.save();

		int index = new ArrayList<>(config.getProfiles().keySet()).indexOf(name);
		if (index >= 0) {
			this.page = index / this.rowsPerPage;
		}
		this.rebuildWidgets();
	}

	private void changePage(int delta) {
		this.page += delta;
		this.rebuildWidgets();
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

		guiGraphics.text(
			this.font,
			this.title.getVisualOrderText(),
			(this.width - this.font.width(this.title)) / 2,
			this.panelTop + 10,
			0xFFFFFFFF,
			true
		);

		if (this.rows.isEmpty()) {
			Component empty = Component.literal("No profiles yet. Type a name and hit Save.").withStyle(ChatFormatting.GRAY);
			guiGraphics.text(
				this.font,
				empty.getVisualOrderText(),
				(this.width - this.font.width(empty)) / 2,
				this.listTop + 6,
				0xFFAAAAAA,
				true
			);
		}

		for (Row row : this.rows) {
			guiGraphics.text(this.font, Component.literal(row.name).getVisualOrderText(), row.x, row.y, 0xFFFFFFFF, true);
			drawArrow(guiGraphics, row.upButton, true);
			drawArrow(guiGraphics, row.downButton, false);
		}

		if (this.statusText != null) {
			guiGraphics.text(
				this.font,
				this.statusText.getVisualOrderText(),
				(this.width - this.font.width(this.statusText)) / 2,
				this.panelBottom - 44,
				0xFFFFFFFF,
				true
			);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xE0101010);
		guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC101018);

		guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFF5C5CFF);
		guiGraphics.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0xFF5C5CFF);
	}

	private static void drawArrow(GuiGraphicsExtractor guiGraphics, Button button, boolean up) {
		int color;
		if (!button.active) {
			color = 0xFF4A4A5A;
		} else if (button.isHovered()) {
			color = 0xFFFFFFFF;
		} else {
			color = 0xFF8C8CFF;
		}

		int centerX = button.getX() + button.getWidth() / 2;
		int top = button.getY() + (button.getHeight() - 4) / 2;
		for (int r = 0; r < 4; r++) {
			int half = up ? r : 3 - r;
			guiGraphics.fill(centerX - half - 1, top + r, centerX + half, top + r + 1, color);
		}
	}

	private record Row(String name, int x, int y, Button upButton, Button downButton) {
	}
}
