package com.eldrinn.tasknh.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

public class Task {

    public final UUID id;
    public String title;
    public String description;
    public TaskStatus status;
    public final List<AssignedPlayer> assignees;
    @Nullable
    public TaskLocation location;
    @Nullable
    public String iconItem; // format: "modid:itemname:meta", e.g. "minecraft:diamond:0"
    public boolean showOnMap = false;
    public final List<Subtask> subtasks;
    public final List<Comment> comments; // soft limit enforced on add: max 50

    public Task(UUID id, String title, String description, TaskStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignees = new ArrayList<>();
        this.location = null;
        this.subtasks = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setString("title", title);
        tag.setString("description", description);
        tag.setString("status", status.name());

        NBTTagList assigneeList = new NBTTagList();
        for (AssignedPlayer ap : assignees) assigneeList.appendTag(ap.toNBT());
        tag.setTag("assignees", assigneeList);

        tag.setBoolean("hasLocation", location != null);
        if (location != null) tag.setTag("location", location.toNBT());

        if (iconItem != null) tag.setString("iconItem", iconItem);
        tag.setBoolean("showOnMap", showOnMap);

        NBTTagList subtaskList = new NBTTagList();
        for (Subtask s : subtasks) subtaskList.appendTag(s.toNBT());
        tag.setTag("subtasks", subtaskList);

        NBTTagList commentList = new NBTTagList();
        for (Comment c : comments) commentList.appendTag(c.toNBT());
        tag.setTag("comments", commentList);

        return tag;
    }

    public static Task fromNBT(NBTTagCompound tag) {
        Task task = new Task(
            new UUID(tag.getLong("idMost"), tag.getLong("idLeast")),
            tag.getString("title"),
            tag.getString("description"),
            TaskStatus.fromNBT(tag.getString("status")));

        NBTTagList assigneeList = tag.getTagList("assignees", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < assigneeList.tagCount(); i++) {
            task.assignees.add(AssignedPlayer.fromNBT(assigneeList.getCompoundTagAt(i)));
        }

        if (tag.getBoolean("hasLocation")) {
            task.location = TaskLocation.fromNBT(tag.getCompoundTag("location"));
        }

        if (tag.hasKey("iconItem")) task.iconItem = tag.getString("iconItem");
        task.showOnMap = tag.getBoolean("showOnMap");

        NBTTagList subtaskList = tag.getTagList("subtasks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < subtaskList.tagCount(); i++) {
            task.subtasks.add(Subtask.fromNBT(subtaskList.getCompoundTagAt(i)));
        }

        NBTTagList commentList = tag.getTagList("comments", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < commentList.tagCount(); i++) {
            task.comments.add(Comment.fromNBT(commentList.getCompoundTagAt(i)));
        }

        return task;
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeStringToBuffer(title);
        buf.writeStringToBuffer(description);
        buf.writeInt(status.ordinal());

        buf.writeInt(assignees.size());
        for (AssignedPlayer ap : assignees) ap.writeToBuf(buf);

        buf.writeBoolean(location != null);
        if (location != null) location.writeToBuf(buf);

        buf.writeStringToBuffer(iconItem != null ? iconItem : "");
        buf.writeBoolean(showOnMap);

        buf.writeInt(subtasks.size());
        for (Subtask s : subtasks) s.writeToBuf(buf);

        buf.writeInt(comments.size());
        for (Comment c : comments) c.writeToBuf(buf);
    }

    public static Task readFromBuf(PacketBuffer buf) throws IOException {
        UUID id = new UUID(buf.readLong(), buf.readLong());
        String title = buf.readStringFromBuffer(256);
        String description = buf.readStringFromBuffer(4096);
        int ordinal = buf.readInt();
        TaskStatus[] statuses = TaskStatus.values();
        if (ordinal < 0 || ordinal >= statuses.length) throw new IOException("Invalid TaskStatus ordinal: " + ordinal);
        TaskStatus status = statuses[ordinal];

        Task task = new Task(id, title, description, status);

        int assigneeCount = buf.readInt();
        if (assigneeCount < 0 || assigneeCount > 100) throw new IOException("Invalid assignee count: " + assigneeCount);
        for (int i = 0; i < assigneeCount; i++) {
            task.assignees.add(AssignedPlayer.readFromBuf(buf));
        }

        if (buf.readBoolean()) {
            task.location = TaskLocation.readFromBuf(buf);
        }

        String icon = buf.readStringFromBuffer(256);
        task.iconItem = icon.isEmpty() ? null : icon;
        task.showOnMap = buf.readBoolean();

        int subtaskCount = buf.readInt();
        if (subtaskCount < 0 || subtaskCount > 200) throw new IOException("Invalid subtask count: " + subtaskCount);
        for (int i = 0; i < subtaskCount; i++) {
            task.subtasks.add(Subtask.readFromBuf(buf));
        }

        int commentCount = buf.readInt();
        if (commentCount < 0 || commentCount > 50) throw new IOException("Invalid comment count: " + commentCount);
        for (int i = 0; i < commentCount; i++) {
            task.comments.add(Comment.readFromBuf(buf));
        }

        return task;
    }
}
