package com.example.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "hotbarpriority")
public class HotbarPriorityConfig implements ConfigData {
    public int slot0Priority = 1;
    public int slot1Priority = 2;
    public int slot2Priority = 3;
    public int slot3Priority = 4;
    public int slot4Priority = 5;
    public int slot5Priority = 6;
    public int slot6Priority = 7;
    public int slot7Priority = 8;
    public int slot8Priority = 9;

    public int[] getSlotPriorities() {
        return new int[]{slot0Priority, slot1Priority, slot2Priority, slot3Priority, slot4Priority, slot5Priority, slot6Priority, slot7Priority, slot8Priority};
    }

    public static int getDefaultPriority(int index) {
        if (index < 0 || index >= 9) {
            throw new IllegalArgumentException("Invalid hotbar slot index: " + index);
        }
        return index + 1;
    }

    public int getSlotPriority(int index) {
        return switch (index) {
            case 0 -> slot0Priority;
            case 1 -> slot1Priority;
            case 2 -> slot2Priority;
            case 3 -> slot3Priority;
            case 4 -> slot4Priority;
            case 5 -> slot5Priority;
            case 6 -> slot6Priority;
            case 7 -> slot7Priority;
            case 8 -> slot8Priority;
            default -> throw new IllegalArgumentException("Invalid hotbar slot index: " + index);
        };
    }

    public void setSlotPriority(int index, int value) {
        switch (index) {
            case 0 -> slot0Priority = value;
            case 1 -> slot1Priority = value;
            case 2 -> slot2Priority = value;
            case 3 -> slot3Priority = value;
            case 4 -> slot4Priority = value;
            case 5 -> slot5Priority = value;
            case 6 -> slot6Priority = value;
            case 7 -> slot7Priority = value;
            case 8 -> slot8Priority = value;
            default -> throw new IllegalArgumentException("Invalid hotbar slot index: " + index);
        }
    }

    public void ensureValid() throws ConfigData.ValidationException {
        boolean[] seen = new boolean[10];
        for (int i = 0; i < 9; i++) {
            int priority = getSlotPriority(i);
            if (priority < 1 || priority > 9) {
                throw new ConfigData.ValidationException("Priorities must be between 1 and 9.");
            }
            if (seen[priority]) {
                throw new ConfigData.ValidationException("Each priority value must be unique.");
            }
            seen[priority] = true;
        }
    }
}
