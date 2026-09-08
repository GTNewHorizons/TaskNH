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
    @Nullable
    public String trackItem; // same format as iconItem; task auto-completes once seen in a member's inventory
    /**
     * Upper bound for {@link #trackItemCount}, enforced wherever the count is set.
     * A main inventory holds 36 slots of at most 64, so a larger count could never be reached.
     * Items that stack lower than 64 top out below this.
     */
    public static final int MAX_TRACK_ITEM_COUNT = 36 * 64;
    /** How many of trackItem one member has to carry. Set from the stack size dragged out of NEI. */
    public int trackItemCount = 1;
    public boolean showOnMap = false;
    /** Parent task id, or null for a root task. Only one nesting level is allowed. */
    @Nullable
    public UUID parentId;
    public final List<ChecklistItem> checklist;
    public final List<Comment> comments; // soft limit enforced on add: max 50

    public Task(UUID id, String title, String description, TaskStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignees = new ArrayList<>();
        this.location = null;
        this.checklist = new ArrayList<>();
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
        if (trackItem != null) tag.setString("trackItem", trackItem);
        if (trackItemCount > 1) tag.setInteger("trackItemCount", trackItemCount);
        tag.setBoolean("showOnMap", showOnMap);
        if (parentId != null) {
            tag.setLong("parentMost", parentId.getMostSignificantBits());
            tag.setLong("parentLeast", parentId.getLeastSignificantBits());
        }

        NBTTagList checklistList = new NBTTagList();
        for (ChecklistItem c : checklist) checklistList.appendTag(c.toNBT());
        tag.setTag("checklist", checklistList);

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
        if (tag.hasKey("trackItem")) task.trackItem = tag.getString("trackItem");
        // Tasks saved before the count existed track a single item.
        task.trackItemCount = tag.hasKey("trackItemCount") ? tag.getInteger("trackItemCount") : 1;
        task.showOnMap = tag.getBoolean("showOnMap");
        if (tag.hasKey("parentMost")) {
            task.parentId = new UUID(tag.getLong("parentMost"), tag.getLong("parentLeast"));
        }

        // "subtasks" is the pre-rename tag, kept so worlds saved before the rename still load.
        String checklistTag = tag.hasKey("checklist") ? "checklist" : "subtasks";
        NBTTagList checklistList = tag.getTagList(checklistTag, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < checklistList.tagCount(); i++) {
            task.checklist.add(ChecklistItem.fromNBT(checklistList.getCompoundTagAt(i)));
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
        buf.writeStringToBuffer(trackItem != null ? trackItem : "");
        buf.writeInt(trackItemCount);
        buf.writeBoolean(showOnMap);
        buf.writeBoolean(parentId != null);
        if (parentId != null) {
            buf.writeLong(parentId.getMostSignificantBits());
            buf.writeLong(parentId.getLeastSignificantBits());
        }

        buf.writeInt(checklist.size());
        for (ChecklistItem c : checklist) c.writeToBuf(buf);

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
        String track = buf.readStringFromBuffer(256);
        task.trackItem = track.isEmpty() ? null : track;
        int trackCount = buf.readInt();
        if (trackCount < 1 || trackCount > MAX_TRACK_ITEM_COUNT)
            throw new IOException("Invalid track item count: " + trackCount);
        task.trackItemCount = trackCount;
        task.showOnMap = buf.readBoolean();
        if (buf.readBoolean()) {
            task.parentId = new UUID(buf.readLong(), buf.readLong());
        }

        int checklistCount = buf.readInt();
        if (checklistCount < 0 || checklistCount > 200)
            throw new IOException("Invalid checklist count: " + checklistCount);
        for (int i = 0; i < checklistCount; i++) {
            task.checklist.add(ChecklistItem.readFromBuf(buf));
        }

        int commentCount = buf.readInt();
        if (commentCount < 0 || commentCount > 50) throw new IOException("Invalid comment count: " + commentCount);
        for (int i = 0; i < commentCount; i++) {
            task.comments.add(Comment.readFromBuf(buf));
        }

        return task;
    }
}
