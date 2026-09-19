package com.eldrinn.tasknh.data;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class ChecklistItem {

    public final UUID id;
    public final String title;
    public boolean checked;
    /** The item gets checked once a member carries this item. Works like {@link Task#trackItem}. */
    @Nullable
    public ItemStack trackItem;
    /** How many of trackItem one member has to carry, see {@link Task#trackItemCount}. */
    public int trackItemCount = 1;
    /**
     * OreDictionary name that any matching item satisfies, or empty for an exact match on trackItem.
     * Only the BetterQuesting import sets it, since a stack dragged from NEI names one item, not a tag.
     * trackItem is then the item the quest shows, kept for the slot to draw.
     */
    public String trackOre = "";

    public ChecklistItem(UUID id, String title, boolean checked) {
        this.id = id;
        this.title = title;
        this.checked = checked;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setString("title", title);
        tag.setBoolean("checked", checked);
        if (trackItem != null) tag.setTag("trackStack", trackItem.writeToNBT(new NBTTagCompound()));
        if (trackItemCount > 1) tag.setInteger("trackItemCount", trackItemCount);
        if (!trackOre.isEmpty()) tag.setString("trackOre", trackOre);
        return tag;
    }

    public static ChecklistItem fromNBT(NBTTagCompound tag) {
        ChecklistItem item = new ChecklistItem(
            new UUID(tag.getLong("idMost"), tag.getLong("idLeast")),
            tag.getString("title"),
            tag.getBoolean("checked"));
        // Items saved before tracking existed have none of these keys and load untracked.
        if (tag.hasKey("trackStack")) item.trackItem = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("trackStack"));
        item.trackItemCount = tag.hasKey("trackItemCount") ? Task.clampTrackItemCount(tag.getInteger("trackItemCount"))
            : 1;
        item.trackOre = tag.getString("trackOre");
        return item;
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeStringToBuffer(title);
        buf.writeBoolean(checked);
        buf.writeItemStackToBuffer(trackItem);
        buf.writeInt(trackItemCount);
        buf.writeStringToBuffer(trackOre);
    }

    public static ChecklistItem readFromBuf(PacketBuffer buf) throws IOException {
        ChecklistItem item = new ChecklistItem(
            new UUID(buf.readLong(), buf.readLong()),
            buf.readStringFromBuffer(256),
            buf.readBoolean());
        item.trackItem = Task.readStack(buf);
        int trackCount = buf.readInt();
        if (trackCount < 1 || trackCount > Task.MAX_TRACK_ITEM_COUNT)
            throw new IOException("Invalid checklist track item count: " + trackCount);
        item.trackItemCount = trackCount;
        item.trackOre = buf.readStringFromBuffer(256);
        return item;
    }
}
