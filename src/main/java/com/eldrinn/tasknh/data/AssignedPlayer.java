package com.eldrinn.tasknh.data;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record AssignedPlayer(UUID playerId, long assignedAt) {

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("most", playerId.getMostSignificantBits());
        tag.setLong("least", playerId.getLeastSignificantBits());
        tag.setLong("assignedAt", assignedAt);
        return tag;
    }

    public static AssignedPlayer fromNBT(NBTTagCompound tag) {
        UUID id = new UUID(tag.getLong("most"), tag.getLong("least"));
        long ts = tag.hasKey("assignedAt") ? tag.getLong("assignedAt") : 0L;
        return new AssignedPlayer(id, ts);
    }

    public void writeToBuf(PacketBuffer buf) {
        buf.writeLong(playerId.getMostSignificantBits());
        buf.writeLong(playerId.getLeastSignificantBits());
        buf.writeLong(assignedAt);
    }

    public static AssignedPlayer readFromBuf(PacketBuffer buf) {
        UUID id = new UUID(buf.readLong(), buf.readLong());
        long ts = buf.readLong();
        return new AssignedPlayer(id, ts);
    }
}
