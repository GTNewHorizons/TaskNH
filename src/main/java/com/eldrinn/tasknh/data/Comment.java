package com.eldrinn.tasknh.data;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Comment(String author, long timestamp, String text) {

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("author", author);
        tag.setLong("timestamp", timestamp);
        tag.setString("text", text);
        return tag;
    }

    public static Comment fromNBT(NBTTagCompound tag) {
        return new Comment(tag.getString("author"), tag.getLong("timestamp"), tag.getString("text"));
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(author);
        buf.writeLong(timestamp);
        buf.writeStringToBuffer(text);
    }

    public static Comment readFromBuf(PacketBuffer buf) throws IOException {
        return new Comment(buf.readStringFromBuffer(64), buf.readLong(), buf.readStringFromBuffer(2048));
    }
}
