package com.eldrinn.tasknh.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.cache.PlayerEntry;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SyncTeamMembersPacket implements IPacket {

    private List<PlayerEntry> members = new ArrayList<>();

    public SyncTeamMembersPacket() {}

    public SyncTeamMembersPacket(List<PlayerEntry> members) {
        this.members = members;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(members.size());
        for (PlayerEntry e : members) {
            buf.writeLong(
                e.id()
                    .getMostSignificantBits());
            buf.writeLong(
                e.id()
                    .getLeastSignificantBits());
            buf.writeStringToBuffer(e.name());
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        if (count < 0 || count > 500) throw new IOException("Invalid member count: " + count);
        members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = new UUID(buf.readLong(), buf.readLong());
            String name = buf.readStringFromBuffer(64);
            members.add(new PlayerEntry(id, name));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        TaskNHClientCache.updateTeamMembers(members);
        return null;
    }
}
