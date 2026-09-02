package com.eldrinn.tasknh.data;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class ChecklistItem {

    public final UUID id;
    public final String title;
    public boolean checked;

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
        return tag;
    }

    public static ChecklistItem fromNBT(NBTTagCompound tag) {
        return new ChecklistItem(
            new UUID(tag.getLong("idMost"), tag.getLong("idLeast")),
            tag.getString("title"),
            tag.getBoolean("checked"));
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeStringToBuffer(title);
        buf.writeBoolean(checked);
    }

    public static ChecklistItem readFromBuf(PacketBuffer buf) throws IOException {
        return new ChecklistItem(
            new UUID(buf.readLong(), buf.readLong()),
            buf.readStringFromBuffer(256),
            buf.readBoolean());
    }
}
