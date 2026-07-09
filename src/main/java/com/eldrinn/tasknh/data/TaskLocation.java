package com.eldrinn.tasknh.data;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class TaskLocation {

    public int dimension;
    public int x, y, z;
    public final String label;

    public TaskLocation(int dimension, int x, int y, int z, String label) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setString("label", label);
        return tag;
    }

    public static TaskLocation fromNBT(NBTTagCompound tag) {
        return new TaskLocation(
            tag.getInteger("dim"),
            tag.getInteger("x"),
            tag.getInteger("y"),
            tag.getInteger("z"),
            tag.getString("label"));
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeStringToBuffer(label);
    }

    public static TaskLocation readFromBuf(PacketBuffer buf) throws IOException {
        return new TaskLocation(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readStringFromBuffer(256));
    }
}
