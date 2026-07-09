package com.eldrinn.tasknh.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.eldrinn.tasknh.config.PinnedTasksConfig;
import com.eldrinn.tasknh.data.Task;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side in-memory store. Replaced wholesale on each sync packet.
 * Only safe to access from the client thread.
 */
@SideOnly(Side.CLIENT)
public class TaskNHClientCache {

    private static final Map<UUID, Task> tasks = new LinkedHashMap<>();
    private static final PinnedTasksConfig pinConfig = new PinnedTasksConfig();
    private static final List<PlayerEntry> teamMembers = new ArrayList<>();

    public static void loadConfig() {
        pinConfig.load();
    }

    public static void update(Collection<Task> incoming) {
        tasks.clear();
        for (Task t : incoming) {
            tasks.put(t.id, t);
        }
        // Remove stale pins in one batch — single save if anything changed
        pinConfig.removeStale(tasks.keySet());
        if (cpw.mods.fml.common.Loader.isModLoaded("navigator")) {
            com.eldrinn.tasknh.navigator.TaskLayerManager.INSTANCE.refreshFromCache(tasks.values());
        }
    }

    public static Collection<Task> getAll() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    @Nullable
    public static Task get(UUID id) {
        return tasks.get(id);
    }

    // --- Pin API ---

    public static void pin(UUID id) {
        pinConfig.pin(id);
    }

    public static void unpin(UUID id) {
        pinConfig.unpin(id);
    }

    public static boolean isPinned(UUID id) {
        return pinConfig.isPinned(id);
    }

    public static boolean canPin() {
        return pinConfig.getPinnedIds()
            .size() < pinConfig.getMaxPinnedTasks();
    }

    public static List<Task> getPinnedTasks() {
        List<Task> result = new ArrayList<>();
        for (UUID id : pinConfig.getPinnedIds()) {
            Task t = tasks.get(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    public static PinnedTasksConfig getPinConfig() {
        return pinConfig;
    }

    public static void updateTeamMembers(List<PlayerEntry> incoming) {
        teamMembers.clear();
        teamMembers.addAll(incoming);
    }

    public static List<PlayerEntry> getTeamMembers() {
        return Collections.unmodifiableList(teamMembers);
    }
}
