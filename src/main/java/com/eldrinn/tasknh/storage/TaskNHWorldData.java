package com.eldrinn.tasknh.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import com.eldrinn.tasknh.data.Task;

public class TaskNHWorldData extends WorldSavedData {

    private static final String DATA_NAME = "TaskNHTasks";

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("tasknh");

    // Outer key = teamId, inner key = taskId
    private final Map<UUID, LinkedHashMap<UUID, Task>> teamTasks = new LinkedHashMap<>();

    private final Map<UUID, Long> playerLastSeen = new LinkedHashMap<>();

    public TaskNHWorldData() {
        super(DATA_NAME);
    }

    // Required by WorldSavedData — called by MapStorage via reflection.
    @SuppressWarnings("unused")
    public TaskNHWorldData(String name) {
        super(name);
    }

    /** Load (or create) the data from the overworld's global map storage. */
    public static TaskNHWorldData get() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) throw new IllegalStateException("TaskNHWorldData.get() called on client side");
        WorldServer overworld = server.worldServerForDimension(0);
        MapStorage storage = overworld.mapStorage;
        TaskNHWorldData data = (TaskNHWorldData) storage.loadData(TaskNHWorldData.class, DATA_NAME);
        if (data == null) {
            data = new TaskNHWorldData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public Collection<Task> getTeamTasks(UUID teamId) {
        LinkedHashMap<UUID, Task> map = teamTasks.get(teamId);
        return map == null ? Collections.emptyList() : Collections.unmodifiableCollection(map.values());
    }

    @Nullable
    public Task getTask(UUID teamId, UUID taskId) {
        LinkedHashMap<UUID, Task> map = teamTasks.get(teamId);
        return map == null ? null : map.get(taskId);
    }

    public void addTask(UUID teamId, Task task) {
        teamTasks.computeIfAbsent(teamId, k -> new LinkedHashMap<>())
            .put(task.id, task);
        markDirty();
    }

    public void updateTask(UUID teamId, Task task) {
        LinkedHashMap<UUID, Task> map = teamTasks.get(teamId);
        if (map != null) {
            map.put(task.id, task);
            markDirty();
        }
    }

    public void deleteTask(UUID teamId, UUID taskId) {
        LinkedHashMap<UUID, Task> map = teamTasks.get(teamId);
        if (map != null && map.remove(taskId) != null) {
            markDirty();
        }
    }

    /** Called on TeamMergeEvent — moves all tasks from consumed team to surviving team. */
    public void mergeTasks(UUID consumedTeamId, UUID survivingTeamId) {
        LinkedHashMap<UUID, Task> consumed = teamTasks.remove(consumedTeamId);
        if (consumed == null || consumed.isEmpty()) return;
        teamTasks.computeIfAbsent(survivingTeamId, k -> new LinkedHashMap<>())
            .putAll(consumed);
        markDirty();
    }

    public long getPlayerLastSeen(UUID playerId) {
        return playerLastSeen.getOrDefault(playerId, 0L);
    }

    public void setPlayerLastSeen(UUID playerId, long timestamp) {
        playerLastSeen.put(playerId, timestamp);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        teamTasks.clear();

        if (compound.hasKey("perTeamTasks")) {
            // New format
            NBTTagList teamList = compound.getTagList("perTeamTasks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < teamList.tagCount(); i++) {
                NBTTagCompound entry = teamList.getCompoundTagAt(i);
                UUID teamId = new UUID(entry.getLong("teamIdMost"), entry.getLong("teamIdLeast"));
                NBTTagList taskList = entry.getTagList("tasks", Constants.NBT.TAG_COMPOUND);
                LinkedHashMap<UUID, Task> map = new LinkedHashMap<>();
                for (int j = 0; j < taskList.tagCount(); j++) {
                    try {
                        Task task = Task.fromNBT(taskList.getCompoundTagAt(j));
                        map.put(task.id, task);
                    } catch (Exception e) {
                        LOG.warn("Skipping corrupt task at team {}, index {}: {}", teamId, j, e.getMessage());
                    }
                }
                teamTasks.put(teamId, map);
            }
        } else if (compound.hasKey("tasks")) {
            // Migration: old flat format — tasks go under a sentinel team UUID
            UUID legacyTeam = UUID.fromString("00000000-0000-0000-0000-000000000000");
            NBTTagList list = compound.getTagList("tasks", Constants.NBT.TAG_COMPOUND);
            LinkedHashMap<UUID, Task> map = new LinkedHashMap<>();
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    Task task = Task.fromNBT(list.getCompoundTagAt(i));
                    map.put(task.id, task);
                } catch (Exception e) {
                    LOG.warn("Skipping corrupt legacy task at index {}: {}", i, e.getMessage());
                }
            }
            if (!map.isEmpty()) {
                teamTasks.put(legacyTeam, map);
                LOG.info("Migrated {} legacy tasks to sentinel team {}", map.size(), legacyTeam);
            }
        }

        playerLastSeen.clear();
        if (compound.hasKey("playerLastSeen")) {
            NBTTagList seenList = compound.getTagList("playerLastSeen", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < seenList.tagCount(); i++) {
                NBTTagCompound e = seenList.getCompoundTagAt(i);
                playerLastSeen.put(new UUID(e.getLong("most"), e.getLong("least")), e.getLong("ts"));
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagList teamList = new NBTTagList();
        for (Map.Entry<UUID, LinkedHashMap<UUID, Task>> teamEntry : teamTasks.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong(
                "teamIdMost",
                teamEntry.getKey()
                    .getMostSignificantBits());
            entry.setLong(
                "teamIdLeast",
                teamEntry.getKey()
                    .getLeastSignificantBits());
            NBTTagList taskList = new NBTTagList();
            for (Task task : teamEntry.getValue()
                .values()) taskList.appendTag(task.toNBT());
            entry.setTag("tasks", taskList);
            teamList.appendTag(entry);
        }
        compound.setTag("perTeamTasks", teamList);

        NBTTagList seenList = new NBTTagList();
        for (Map.Entry<UUID, Long> entry : playerLastSeen.entrySet()) {
            NBTTagCompound e = new NBTTagCompound();
            e.setLong(
                "most",
                entry.getKey()
                    .getMostSignificantBits());
            e.setLong(
                "least",
                entry.getKey()
                    .getLeastSignificantBits());
            e.setLong("ts", entry.getValue());
            seenList.appendTag(e);
        }
        compound.setTag("playerLastSeen", seenList);
    }
}
