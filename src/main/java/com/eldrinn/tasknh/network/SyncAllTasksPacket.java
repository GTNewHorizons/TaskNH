package com.eldrinn.tasknh.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SyncAllTasksPacket implements IPacket {

    private List<Task> tasks = new ArrayList<>();

    public SyncAllTasksPacket() {}

    public SyncAllTasksPacket(Collection<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(tasks.size());
        for (Task task : tasks) {
            task.writeToBuf(buf);
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tasks.add(Task.readFromBuf(buf));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        TaskNHClientCache.update(tasks);
        TaskNHGui.notifySyncReceived();
        return null;
    }
}
