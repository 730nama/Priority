package aman037.priority;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Config(name = "hotbarpriority")
public class PriorityConfig implements ConfigData {
    public int slot0Priority = 1;
    public int slot1Priority = 2;
    public int slot2Priority = 3;
    public int slot3Priority = 4;
    public int slot4Priority = 5;
    public int slot5Priority = 6;
    public int slot6Priority = 7;
    public int slot7Priority = 8;
    public int slot8Priority = 9;

    public Map<String, int[]> profiles = new LinkedHashMap<>();

    // slot n defaults to priority n+1, ie. plain left to right order
    public static int getDefaultPriority(int index) {
        return index + 1;
    }

    public Map<String, int[]> getProfiles() {
        // 1.0.0 configs dont have this
        if (profiles == null) {
            profiles = new LinkedHashMap<>();
        }
        return profiles;
    }

    public void saveProfile(String name, int[] values) {
        getProfiles().put(name, values.clone());
    }

    public int[] getProfile(String name) {
        int[] values = getProfiles().get(name);
        return values == null ? null : values.clone();
    }

    public void deleteProfile(String name) {
        getProfiles().remove(name);
    }

    public void moveProfile(String name, int delta) {
        List<String> names = new ArrayList<>(getProfiles().keySet());
        int from = names.indexOf(name);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= names.size()) {
            return;
        }
        Collections.swap(names, from, to);

        Map<String, int[]> reordered = new LinkedHashMap<>();
        for (String n : names) {
            reordered.put(n, profiles.get(n));
        }
        profiles = reordered;
    }

    public static boolean isValidProfile(int[] values) {
        if (values == null || values.length != 9) {
            return false;
        }
        boolean[] seen = new boolean[10];
        for (int v : values) {
            if (v < 1 || v > 9 || seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
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
            default -> throw new IllegalArgumentException("bad slot index " + index);
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
            default -> throw new IllegalArgumentException("bad slot index " + index);
        }
    }

    // AutoConfig calls this before saving
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
