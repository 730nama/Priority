package aman037.priority;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConfigScreen extends Screen {
	private static final int LABEL_WIDTH = 64;
	private static final int FIELD_WIDTH = 30;
	private static final int FIELD_HEIGHT = 20;
	private static final int PANEL_MARGIN = 22;
	private static final int ROW_SPACING = 30;
	private static final int SLOT_COUNT = 9;

	private final Screen parent;
	private final ConfigHolder<PriorityConfig> holder;
	private final List<SlotField> slotFields = new ArrayList<>();

	private Button saveButton;
	private Component errorText;
	private int[] pendingValues;
	private int baseY;
	private int firstColumnX;
	private int panelLeft;
	private int panelRight;
	private int panelTop;
	private int panelBottom;
	private int columnCount;
	private int rowsPerColumn;
	private int columnContentWidth;
	private int labelColumnWidth;

	public ConfigScreen(Screen parent) {
		super(Component.literal("Hotbar Priority"));
		this.parent = parent;
		this.holder = AutoConfig.getConfigHolder(PriorityConfig.class);
	}

	@Override
	protected void init() {
		super.init();
		this.slotFields.clear();

		int centerX = this.width / 2;

		List<Component> labels = new ArrayList<>(SLOT_COUNT);
		int maxLabelTextWidth = 0;
		for (int slot = 0; slot < SLOT_COUNT; slot++) {
			Component label = Component.literal("Slot " + (slot + 1));
			labels.add(label);
			maxLabelTextWidth = Math.max(maxLabelTextWidth, this.font.width(label));
		}

		this.labelColumnWidth = Math.max(LABEL_WIDTH, maxLabelTextWidth + 6);
		int columnInnerWidth = this.labelColumnWidth + 6 + FIELD_WIDTH + 6 + 42;
		int usableWidth = Math.max(columnInnerWidth, this.width - PANEL_MARGIN * 2);

		int desiredColumns = Math.min(2, Math.max(1, (int) Math.ceil(SLOT_COUNT / 5.0)));
		int columns = desiredColumns;
		while (columns > 1) {
			int requiredWidth = columns * columnInnerWidth + (columns - 1) * 28;
			if (requiredWidth <= usableWidth) {
				break;
			}
			columns--;
		}
		this.columnCount = Math.max(1, columns);
		this.rowsPerColumn = (int) Math.ceil(SLOT_COUNT / (double) this.columnCount);
		this.columnContentWidth = columnInnerWidth;

		int totalColumnsWidth = this.columnCount * this.columnContentWidth + (this.columnCount - 1) * 28;
		this.firstColumnX = centerX - totalColumnsWidth / 2;
		this.panelLeft = Math.max(24, this.firstColumnX - PANEL_MARGIN);
		this.panelRight = Math.min(this.width - 24, this.firstColumnX + totalColumnsWidth + PANEL_MARGIN);

		int contentHeight = Math.max(5, this.rowsPerColumn) * ROW_SPACING;
		int idealBaseY = (this.height - contentHeight) / 2;
		int minBaseY = 72;
		int maxBaseY = Math.max(minBaseY, this.height - 96 - contentHeight - 24);
		this.baseY = Math.min(Math.max(idealBaseY, minBaseY), maxBaseY);
		this.panelTop = Math.max(24, this.baseY - 48);
		this.panelBottom = Math.min(this.height - 24, this.baseY + contentHeight + 96);

		PriorityConfig config = holder.getConfig();

		for (int slot = 0; slot < SLOT_COUNT; slot++) {
			int column = slot / this.rowsPerColumn;
			if (column >= this.columnCount) {
				column = this.columnCount - 1;
			}
			int row = slot % this.rowsPerColumn;

			int columnX = this.firstColumnX + column * (this.columnContentWidth + 28);
			int labelX = columnX;
			int fieldX = labelX + this.labelColumnWidth + 6;
			int y = this.baseY + row * ROW_SPACING;
			int labelY = y + (FIELD_HEIGHT - this.font.lineHeight) / 2;
			int resetX = fieldX + FIELD_WIDTH + 6;

			Component label = labels.get(slot);
			EditBox field = new EditBox(this.font, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
			field.setMaxLength(1);
			field.setResponder(text -> {
				if (!text.isEmpty() && !text.matches("[0-9]")) {
					field.setValue(text.replaceAll("[^0-9]", ""));
				}
				updateState();
			});
			field.setValue(String.valueOf(this.pendingValues != null ? this.pendingValues[slot] : config.getSlotPriority(slot)));

			final EditBox finalField = field;
			final int finalSlot = slot;
			Button resetButton = Button.builder(Component.literal("Reset"), b -> resetSlotToDefault(finalField, finalSlot))
				.bounds(resetX, y, 42, 20)
				.build();

			SlotField entry = new SlotField(slot, label, field, labelX, labelY, resetButton);
			this.slotFields.add(entry);
			this.addRenderableWidget(field);
			this.addRenderableWidget(resetButton);
		}
		this.pendingValues = null;

		this.addRenderableWidget(Button.builder(Component.literal("Profiles..."), b -> openProfiles())
			.bounds(centerX - 50, this.panelBottom - 28, 100, 20)
			.build());

		this.saveButton = null;

		updateState();
	}

	void loadValues(int[] values) {
		this.pendingValues = values.clone();
	}

	int[] currentValues() {
		if (validateInput().isPresent()) {
			return null;
		}
		int[] values = new int[SLOT_COUNT];
		for (SlotField slotField : this.slotFields) {
			values[slotField.slotIndex] = Integer.parseInt(slotField.field.getValue());
		}
		return values;
	}

	private void openProfiles() {
		// init() wipes the fields otherwise
		int[] current = currentValues();
		if (current != null) {
			this.pendingValues = current;
		}
		Minecraft.getInstance().gui.setScreen(new ProfilesScreen(this, current));
	}

	private void save() {
		Optional<Component> validationError = validateInput();
		if (validationError.isPresent()) {
			this.errorText = validationError.get();
			updateState();
			return;
		}

		PriorityConfig config = holder.getConfig();
		for (SlotField slotField : this.slotFields) {
			int value = Integer.parseInt(slotField.field.getValue());
			config.setSlotPriority(slotField.slotIndex, value);
		}

		commitConfig(config);
		Minecraft.getInstance().gui.setScreen(null);
	}

	private void cancel() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}

	private void updateState() {
		Optional<Component> validationError = validateInput();
		this.errorText = validationError.orElse(null);
		if (this.saveButton != null) {
			this.saveButton.active = validationError.isEmpty();
		}
	}

	private Optional<Component> validateInput() {
		Set<Integer> seen = new HashSet<>();
		for (SlotField slotField : this.slotFields) {
			String raw = slotField.field.getValue();
			if (raw == null || raw.isEmpty()) {
				return Optional.of(Component.literal("All slots must have a priority."));
			}
			int value;
			try {
				value = Integer.parseInt(raw);
			} catch (NumberFormatException ex) {
				return Optional.of(Component.literal("Priorities must be between 1 and 9."));
			}
			if (value < 1 || value > 9) {
				return Optional.of(Component.literal("Priorities must be between 1 and 9."));
			}
			if (!seen.add(value)) {
				return Optional.of(Component.literal("Priority " + value + " is assigned more than once."));
			}
		}
		return Optional.empty();
	}

	private boolean commitConfig(PriorityConfig config) {
		try {
			config.ensureValid();
			holder.save();
			return true;
		} catch (me.shedaniel.autoconfig.ConfigData.ValidationException e) {
			this.errorText = Component.literal("Resolve duplicate priorities before saving.").withStyle(ChatFormatting.RED);
			return false;
		}
	}

	private void resetSlotToDefault(EditBox field, int slotIndex) {
		field.setValue(String.valueOf(PriorityConfig.getDefaultPriority(slotIndex)));
		updateState();
	}

	@Override
	public void onClose() {
		Optional<Component> validationError = validateInput();
		if (validationError.isEmpty()) {
			PriorityConfig config = holder.getConfig();
			for (SlotField slotField : this.slotFields) {
				int value = Integer.parseInt(slotField.field.getValue());
				config.setSlotPriority(slotField.slotIndex, value);
			}
			if (!commitConfig(config)) {
				return;
			}
		} else {
			this.errorText = validationError.get();
			updateState();
			return;
		}
		Minecraft.getInstance().gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

		guiGraphics.text(
			this.font,
			this.title.getVisualOrderText(),
			(this.width - this.font.width(this.title)) / 2,
			panelTop - 18,
			0xFFFFFFFF,
			true
		);

		MutableComponent instructions = Component.literal("Enter unique values 1-9 to set hotbar priority order.").withStyle(ChatFormatting.GRAY);
		guiGraphics.text(
			this.font,
			instructions.getVisualOrderText(),
			(this.width - this.font.width(instructions)) / 2,
			panelTop + 14,
			0xFFAAAAAA,
			true
		);

		for (SlotField slotField : this.slotFields) {
			guiGraphics.text(
				this.font,
				slotField.label.getVisualOrderText(),
				slotField.labelX,
				slotField.labelY,
				0xFFFFFFFF,
				true
			);
		}

		if (this.errorText != null) {
			guiGraphics.text(
				this.font,
				this.errorText.getVisualOrderText(),
				(this.width - this.font.width(this.errorText)) / 2,
				this.panelBottom - 38,
				0xFFFF5555,
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

	private static final class SlotField {
		final int slotIndex;
		final Component label;
		final EditBox field;
		final int labelX;
		final int labelY;
		final Button resetButton;

		SlotField(int slotIndex, Component label, EditBox field, int labelX, int labelY, Button resetButton) {
			this.slotIndex = slotIndex;
			this.label = label;
			this.field = field;
			this.labelX = labelX;
			this.labelY = labelY;
			this.resetButton = resetButton;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ModMenuIntegration implements ModMenuApi {
		@Override
		public ConfigScreenFactory<?> getModConfigScreenFactory() {
			return ConfigScreen::new;
		}
	}
}
