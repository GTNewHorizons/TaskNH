package com.eldrinn.tasknh.network;

import java.util.UUID;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Sent on login ahead of the first sync, so the client keeps pins and folds apart per world. */
public class WorldIdPacket implements IPacket {

    private UUID worldId;

    public WorldIdPacket() {}

    public WorldIdPacket(UUID worldId) {
        this.worldId = worldId;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeLong(worldId.getMostSignificantBits());
        buf.writeLong(worldId.getLeastSignificantBits());
    }

    @Override
    public void decode(PacketBuffer buf) {
        worldId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        TaskNHClientCache.setWorld(worldId);
        return null;
    }
}
