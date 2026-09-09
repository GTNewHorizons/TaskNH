package com.eldrinn.tasknh.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    /** Shown next to the title. Kept as a stack so items that differ only by NBT keep their own icon. */
    @Nullable
    public ItemStack iconItem;
    /** The task auto-completes once a member carries this item. */
    @Nullable
    public ItemStack trackItem;
    /**
     * Upper bound for {@link #trackItemCount}, enforced wherever the count is set.
     * A main inventory holds 36 slots of at most 64, so a larger count could never be reached.
     * Items that stack lower than 64 top out below this.
     */
    public static final int MAX_TRACK_ITEM_COUNT = 36 * 64;
    /**
     * How many of trackItem one member has to carry. Kept apart from the stack, which stores its size
     * in a single byte and could not hold the range this allows.
     */
    public int trackItemCount = 1;
    public boolean showOnMap = false;
    /** Parent task id, or null for a root task. Only one nesting level is allowed. */
    @Nullable
    public UUID parentId;
    /** Manual sort position among siblings, shared by the whole team. Lower comes first. */
    public int order;
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

    /** Holds a count inside the range {@link #readFromBuf} accepts. Every path that sets one goes through here. */
    public static int clampTrackItemCount(int count) {
        return Math.min(MAX_TRACK_ITEM_COUNT, Math.max(1, count));
    }

    /** Reads the "modid:item:meta" string tasks used before items were stored as stacks. */
    @Nullable
    public static ItemStack parseLegacyItem(String value) {
        if (value == null || value.isEmpty()) return null;
        String[] parts = value.split(":");
        if (parts.length < 3) return null;
        Item item = (Item) Item.itemRegistry.getObject(parts[0] + ":" + parts[1]);
        if (item == null) return null;
        try {
            return new ItemStack(item, 1, Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
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

        if (iconItem != null) tag.setTag("iconStack", iconItem.writeToNBT(new NBTTagCompound()));
        if (trackItem != null) tag.setTag("trackStack", trackItem.writeToNBT(new NBTTagCompound()));
        if (trackItemCount > 1) tag.setInteger("trackItemCount", trackItemCount);
        tag.setBoolean("showOnMap", showOnMap);
        tag.setInteger("order", order);
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

        // "iconItem" and "trackItem" are the pre-NBT tags, a "modid:item:meta" string. Worlds saved with them
        // keep loading, and the task is written back in the stack form on the next save.
        task.iconItem = tag.hasKey("iconStack") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("iconStack"))
            : parseLegacyItem(tag.getString("iconItem"));
        task.trackItem = tag.hasKey("trackStack") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("trackStack"))
            : parseLegacyItem(tag.getString("trackItem"));
        // Tasks saved before the count existed track a single item. A save edited by hand can hold
        // anything, and an out of range count would later fail to decode on the client, so clamp on load.
        task.trackItemCount = tag.hasKey("trackItemCount") ? clampTrackItemCount(tag.getInteger("trackItemCount")) : 1;
        task.showOnMap = tag.getBoolean("showOnMap");
        // Worlds saved before manual ordering have no key, so everything starts at 0 and keeps its old order.
        task.order = tag.getInteger("order");
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

        buf.writeItemStackToBuffer(iconItem);
        buf.writeItemStackToBuffer(trackItem);
        buf.writeInt(trackItemCount);
        buf.writeBoolean(showOnMap);
        buf.writeInt(order);
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

        task.iconItem = buf.readItemStackFromBuffer();
        task.trackItem = buf.readItemStackFromBuffer();
        int trackCount = buf.readInt();
        if (trackCount < 1 || trackCount > MAX_TRACK_ITEM_COUNT)
            throw new IOException("Invalid track item count: " + trackCount);
        task.trackItemCount = trackCount;
        task.showOnMap = buf.readBoolean();
        task.order = buf.readInt();
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
