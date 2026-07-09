package com.eldrinn.tasknh.network;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class OpenGuiPacket implements IPacket {

    @Nullable
    private UUID taskId;

    public OpenGuiPacket() {
        this.taskId = null;
    }

    public OpenGuiPacket(@Nullable UUID taskId) {
        this.taskId = taskId;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeBoolean(taskId != null);
        if (taskId != null) {
            buf.writeLong(taskId.getMostSignificantBits());
            buf.writeLong(taskId.getLeastSignificantBits());
        }
    }

    @Override
    public void decode(PacketBuffer buf) {
        if (buf.readBoolean()) {
            taskId = new UUID(buf.readLong(), buf.readLong());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        if (taskId != null) {
            TaskNHGuiData data = new TaskNHGuiData();
            data.selectTask(taskId);
            TaskNHGui.open(data);
        } else {
            TaskNHGui.open();
        }
        return null;
    }
}
