package com.eldrinn.tasknh.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PinnedTasksConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    /** How many subtasks a parent shows in the task list. The fold button cycles through them in this order. */
    public enum Fold {
        SHOW_ALL,
        HIDE_DONE,
        HIDE_ALL
    }

    private static class Data {

        // Pins and folds from before they were kept per world. Moved into the first world joined, then dropped:
        // gson skips null fields when writing.
        @SerializedName("pinnedTasks")
        List<String> legacyPinnedTasks;

        @SerializedName("foldedTasks")
        Map<String, String> legacyFoldedTasks;

        /** Keyed by the world id the server sends on login, see WorldIdPacket. */
        @SerializedName("worlds")
        Map<String, WorldEntry> worlds = new HashMap<>();

        @SerializedName("hud")
        HudPosition hud = new HudPosition();
    }

    private static class WorldEntry {

        @SerializedName("pinnedTasks")
        List<String> pinnedTasks = new ArrayList<>();

        // Only parents that differ from the default SHOW_ALL are stored.
        @SerializedName("foldedTasks")
        Map<String, String> foldedTasks = new HashMap<>();
    }

    private static class HudPosition {

        @SerializedName("anchor")
        String anchor = Anchor.TOP_RIGHT.name();

        @SerializedName("offsetX")
        int offsetX = 0;

        @SerializedName("offsetY")
        int offsetY = 0;

        @SerializedName("scale")
        double scale = 1.0;

        @SerializedName("showBackground")
        boolean showBackground = true;

        @SerializedName("hudVisible")
        boolean hudVisible = true;

        @SerializedName("maxChecklistShown")
        int maxChecklistShown = 3;

        // Pre-rename key. Read once on load, then dropped: gson skips null fields when writing.
        @SerializedName("maxSubtasksShown")
        Integer legacyMaxSubtasksShown;

        @SerializedName("maxPinnedTasks")
        int maxPinnedTasks = 5;
    }

    private Data data = new Data();

    /** Entry of the world the player is in, or null until the server sends its id. */
    @Nullable
    private WorldEntry world = null;

    public void load() {
        File file = configFile();
        boolean migrating = false;
        if (!file.exists()) {
            // One-time migration from the pre-rename config file.
            File legacy = legacyConfigFile();
            if (!legacy.exists()) return;
            file = legacy;
            migrating = true;
        }
        try (FileReader reader = new FileReader(file)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                data = loaded;
                if (data.hud == null) data.hud = new HudPosition();
                if (data.worlds == null) data.worlds = new HashMap<>();
            }
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger("tasknh")
                .warn("Failed to load tasknh_pins.json: {}", e.getMessage());
        }
        if (data.hud.legacyMaxSubtasksShown != null) {
            data.hud.maxChecklistShown = data.hud.legacyMaxSubtasksShown;
            data.hud.legacyMaxSubtasksShown = null;
            migrating = true;
        }
        if (migrating) save(); // persist to the new tasknh_pins.json
    }

    public void save() {
        File file = configFile();
        File parentDir = file.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            org.apache.logging.log4j.LogManager.getLogger("tasknh")
                .warn("Could not create config directory: {}", parentDir);
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger("tasknh")
                .warn("Failed to save tasknh_pins.json: {}", e.getMessage());
        }
    }

    /** Switches pins and folds to the given world, and moves the pre-per-world ones into it once. */
    public void setWorld(UUID worldId) {
        WorldEntry entry = data.worlds.get(worldId.toString());
        boolean changed = false;
        if (entry == null) {
            entry = new WorldEntry();
            data.worlds.put(worldId.toString(), entry);
            changed = true;
        }
        // A hand-edited file can carry explicit nulls, which gson keeps.
        if (entry.pinnedTasks == null) entry.pinnedTasks = new ArrayList<>();
        if (entry.foldedTasks == null) entry.foldedTasks = new HashMap<>();
        // The next sync drops whatever of these belongs to another world.
        if (data.legacyPinnedTasks != null) {
            entry.pinnedTasks.addAll(data.legacyPinnedTasks);
            data.legacyPinnedTasks = null;
            changed = true;
        }
        if (data.legacyFoldedTasks != null) {
            entry.foldedTasks.putAll(data.legacyFoldedTasks);
            data.legacyFoldedTasks = null;
            changed = true;
        }
        world = entry;
        if (changed) save();
    }

    public List<UUID> getPinnedIds() {
        List<UUID> result = new ArrayList<>();
        if (world == null) return result;
        for (String s : world.pinnedTasks) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    public boolean isPinned(UUID id) {
        return world != null && world.pinnedTasks.contains(id.toString());
    }

    public void pin(UUID id) {
        if (world == null) return;
        String s = id.toString();
        if (world.pinnedTasks.contains(s)) return;
        if (world.pinnedTasks.size() >= data.hud.maxPinnedTasks) return;
        world.pinnedTasks.add(s);
        save();
    }

    public void unpin(UUID id) {
        if (world != null && world.pinnedTasks.remove(id.toString())) {
            save();
        }
    }

    public Fold getFold(UUID id) {
        String value = world == null ? null : world.foldedTasks.get(id.toString());
        if (value == null) return Fold.SHOW_ALL;
        try {
            return Fold.valueOf(value);
        } catch (IllegalArgumentException e) {
            return Fold.SHOW_ALL;
        }
    }

    public void setFold(UUID id, Fold fold) {
        if (world == null) return;
        if (fold == Fold.SHOW_ALL) {
            world.foldedTasks.remove(id.toString());
        } else {
            world.foldedTasks.put(id.toString(), fold.name());
        }
        save();
    }

    /** Drops pins and folds of tasks the current world no longer has. Other worlds' entries stay untouched. */
    public void removeStale(java.util.Set<UUID> existing) {
        if (world == null) return;
        java.util.function.Predicate<String> stale = s -> {
            try {
                return !existing.contains(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                return true; // remove malformed entries too
            }
        };
        boolean changed = world.pinnedTasks.removeIf(stale);
        changed |= world.foldedTasks.keySet()
            .removeIf(stale);
        if (changed) save();
    }

    public Anchor getAnchor() {
        try {
            return Anchor.valueOf(data.hud.anchor);
        } catch (IllegalArgumentException e) {
            return Anchor.TOP_RIGHT;
        }
    }

    public int getOffsetX() {
        return data.hud.offsetX;
    }

    public int getOffsetY() {
        return data.hud.offsetY;
    }

    public double getScale() {
        return data.hud.scale;
    }

    public void setScale(double scale) {
        data.hud.scale = Math.max(0.5, Math.min(2.0, scale));
        save();
    }

    public boolean isShowBackground() {
        return data.hud.showBackground;
    }

    public void setShowBackground(boolean showBackground) {
        data.hud.showBackground = showBackground;
        save();
    }

    public boolean isHudVisible() {
        return data.hud.hudVisible;
    }

    public void setHudVisible(boolean hudVisible) {
        data.hud.hudVisible = hudVisible;
        save();
    }

    /** Writes offsetX to memory only — caller must call save() when drag ends. */
    public void setOffsetXRaw(int offsetX) {
        data.hud.offsetX = offsetX;
    }

    /** Writes offsetY to memory only — caller must call save() when drag ends. */
    public void setOffsetYRaw(int offsetY) {
        data.hud.offsetY = offsetY;
    }

    public void setAnchor(Anchor anchor) {
        data.hud.anchor = anchor.name();
    }

    public void resetToDefaults() {
        data.hud.anchor = Anchor.TOP_RIGHT.name();
        data.hud.offsetX = 0;
        data.hud.offsetY = 0;
        data.hud.scale = 1.0;
        data.hud.showBackground = true;
        data.hud.hudVisible = true;
        data.hud.maxChecklistShown = 3;
        data.hud.maxPinnedTasks = 5;
        save();
    }

    public int getMaxChecklistShown() {
        return data.hud.maxChecklistShown;
    }

    public void setMaxChecklistShown(int value) {
        data.hud.maxChecklistShown = Math.max(1, Math.min(10, value));
        save();
    }

    public int getMaxPinnedTasks() {
        return data.hud.maxPinnedTasks;
    }

    public void setMaxPinnedTasks(int value) {
        data.hud.maxPinnedTasks = Math.max(1, Math.min(10, value));
        save();
    }

    private static File configFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/tasknh_pins.json");
    }

    // Pre-rename config file (mod was formerly "Foreman"). Kept for one-time migration.
    private static File legacyConfigFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/foreman_pins.json");
    }
}
