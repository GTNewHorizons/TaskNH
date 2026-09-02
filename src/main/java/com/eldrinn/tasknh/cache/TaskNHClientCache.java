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

    /**
     * Edit we made locally and sent to the server, applied to the cache right away and kept until
     * the server confirms it. A sync caused by an earlier edit carries server state that predates
     * this one and would otherwise undo it.
     */
    @Nullable
    private static Task pendingEdit = null;

    @Nullable
    private static UUID pendingDelete = null;

    /** Applies an edit locally, so the GUI shows it without waiting for the server. */
    public static void putLocal(Task task) {
        tasks.put(task.id, task);
        pendingEdit = task;
        pendingDelete = null;
    }

    /** Applies a deletion locally, cascading to subtasks the way the server does. */
    public static void removeLocal(UUID taskId) {
        tasks.remove(taskId);
        tasks.values()
            .removeIf(t -> taskId.equals(t.parentId));
        pendingDelete = taskId;
        if (pendingEdit != null && pendingEdit.id.equals(taskId)) pendingEdit = null;
    }

    public static void loadConfig() {
        pinConfig.load();
    }

    public static void update(Collection<Task> incoming) {
        tasks.clear();
        for (Task t : incoming) {
            tasks.put(t.id, t);
        }
        if (com.eldrinn.tasknh.gui.TaskNHGui.isWithinSelfEditWindow()) {
            if (pendingEdit != null) tasks.put(pendingEdit.id, pendingEdit);
            if (pendingDelete != null) removeLocal(pendingDelete);
        } else {
            // Confirmed by now, server state wins again.
            pendingEdit = null;
            pendingDelete = null;
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
