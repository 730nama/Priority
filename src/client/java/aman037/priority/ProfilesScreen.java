package aman037.priority;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

	private TextFieldWidget nameField;
	private ButtonWidget saveButton;
	private Text statusText;
	private int page;
	private int rowsPerPage;
	private int listTop;
	private int panelLeft;
	private int panelRight;
	private int panelTop;
	private int panelBottom;

	public ProfilesScreen(ConfigScreen parent, int[] currentValues) {
		super(Text.literal("Priority Profiles"));
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

		String oldName = this.nameField != null ? this.nameField.getText() : "";
		int topY = this.panelTop + 30;
		this.nameField = new TextFieldWidget(this.textRenderer, left, topY, LIST_WIDTH - 66, 20, Text.literal("Profile name"));
		this.nameField.setMaxLength(MAX_NAME_LENGTH);
		this.nameField.setPlaceholder(Text.literal("Profile name").formatted(Formatting.DARK_GRAY));
		this.nameField.setText(oldName);
		this.nameField.setChangedListener(text -> updateSaveButton());
		this.addDrawableChild(this.nameField);

		this.saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveProfile())
			.dimensions(left + LIST_WIDTH - 60, topY, 60, 20)
			.build();
		this.addDrawableChild(this.saveButton);

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

			ButtonWidget loadButton = ButtonWidget.builder(Text.literal("Load"), b -> loadProfile(name))
				.dimensions(left + LIST_WIDTH - 122, y, 50, 20)
				.build();
			ButtonWidget upButton = ButtonWidget.builder(Text.empty(), b -> moveProfile(name, -1))
				.dimensions(left + LIST_WIDTH - 68, y, 14, 10)
				.build();
			ButtonWidget downButton = ButtonWidget.builder(Text.empty(), b -> moveProfile(name, 1))
				.dimensions(left + LIST_WIDTH - 68, y + 10, 14, 10)
				.build();
			upButton.active = i > 0;
			downButton.active = i < names.size() - 1;
			ButtonWidget deleteButton = ButtonWidget.builder(Text.literal("Delete").formatted(Formatting.RED), b -> deleteProfile(name))
				.dimensions(left + LIST_WIDTH - 50, y, 50, 20)
				.build();
			this.addDrawableChild(loadButton);
			this.addDrawableChild(upButton);
			this.addDrawableChild(downButton);
			this.addDrawableChild(deleteButton);

			String shown = name;
			int maxNameWidth = LIST_WIDTH - 128;
			if (this.textRenderer.getWidth(shown) > maxNameWidth) {
				shown = this.textRenderer.trimToWidth(shown, maxNameWidth - this.textRenderer.getWidth("...")) + "...";
			}
			this.rows.add(new Row(shown, left, y + (20 - this.textRenderer.fontHeight) / 2, upButton, downButton));
		}

		int bottomY = this.panelBottom - 28;
		if (pageCount > 1) {
			ButtonWidget prev = ButtonWidget.builder(Text.literal("<"), b -> changePage(-1))
				.dimensions(left, bottomY - 24, 20, 20)
				.build();
			ButtonWidget next = ButtonWidget.builder(Text.literal(">"), b -> changePage(1))
				.dimensions(left + LIST_WIDTH - 20, bottomY - 24, 20, 20)
				.build();
			prev.active = this.page > 0;
			next.active = this.page < pageCount - 1;
			this.addDrawableChild(prev);
			this.addDrawableChild(next);
		}

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
			.dimensions(centerX - 50, bottomY, 100, 20)
			.build());

		updateSaveButton();
	}

	private void updateSaveButton() {
		if (this.saveButton != null) {
			this.saveButton.active = this.currentValues != null && !this.nameField.getText().isBlank();
		}
	}

	private void saveProfile() {
		if (this.currentValues == null) {
			this.statusText = Text.literal("Fix the priorities first, they arent valid.").formatted(Formatting.RED);
			return;
		}
		String name = this.nameField.getText().trim();
		if (name.isEmpty()) {
			return;
		}

		PriorityConfig config = this.holder.getConfig();
		boolean overwrite = config.getProfiles().containsKey(name);
		config.saveProfile(name, this.currentValues);
		this.holder.save();

		this.statusText = Text.literal((overwrite ? "Overwrote " : "Saved ") + "\"" + name + "\"").formatted(Formatting.GREEN);
		this.nameField.setText("");
		this.clearAndInit();
	}

	private void loadProfile(String name) {
		int[] values = this.holder.getConfig().getProfile(name);
		if (!PriorityConfig.isValidProfile(values)) {
			this.statusText = Text.literal("\"" + name + "\" is broken, check the config file.").formatted(Formatting.RED);
			return;
		}
		// still have to close the main screen for it to save
		this.parent.loadValues(values);
		MinecraftClient.getInstance().setScreen(this.parent);
	}

	private void deleteProfile(String name) {
		MinecraftClient.getInstance().setScreen(new ConfirmScreen(
			confirmed -> {
				if (confirmed) {
					this.holder.getConfig().deleteProfile(name);
					this.holder.save();
					this.statusText = Text.literal("Deleted \"" + name + "\"").formatted(Formatting.YELLOW);
				}
				MinecraftClient.getInstance().setScreen(this);
			},
			Text.literal("Delete profile?"),
			Text.literal("\"" + name + "\" will be gone for good.")
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
		this.clearAndInit();
	}

	private void changePage(int delta) {
		this.page += delta;
		this.clearAndInit();
	}

	@Override
	public void close() {
		MinecraftClient.getInstance().setScreen(this.parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawText(
			this.textRenderer,
			this.title,
			(this.width - this.textRenderer.getWidth(this.title)) / 2,
			this.panelTop + 10,
			0xFFFFFFFF,
			true
		);

		if (this.rows.isEmpty()) {
			Text empty = Text.literal("No profiles yet. Type a name and hit Save.").formatted(Formatting.GRAY);
			context.drawText(
				this.textRenderer,
				empty,
				(this.width - this.textRenderer.getWidth(empty)) / 2,
				this.listTop + 6,
				0xFFAAAAAA,
				true
			);
		}

		for (Row row : this.rows) {
			context.drawText(this.textRenderer, Text.literal(row.name), row.x, row.y, 0xFFFFFFFF, true);
			drawArrow(context, row.upButton, true);
			drawArrow(context, row.downButton, false);
		}

		if (this.statusText != null) {
			context.drawText(
				this.textRenderer,
				this.statusText,
				(this.width - this.textRenderer.getWidth(this.statusText)) / 2,
				this.panelBottom - 44,
				0xFFFFFFFF,
				true
			);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xE0101010);
		context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC101018);

		context.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFF5C5CFF);
		context.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0xFF5C5CFF);
	}

	private static void drawArrow(DrawContext context, ButtonWidget button, boolean up) {
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
			context.fill(centerX - half - 1, top + r, centerX + half, top + r + 1, color);
		}
	}

	private record Row(String name, int x, int y, ButtonWidget upButton, ButtonWidget downButton) {
	}
}
