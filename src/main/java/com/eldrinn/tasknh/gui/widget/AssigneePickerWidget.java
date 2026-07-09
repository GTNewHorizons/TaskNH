package com.eldrinn.tasknh.gui.widget;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.tasknh.cache.PlayerEntry;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AssigneePickerWidget extends Flow {

    public AssigneePickerWidget(Task task, TaskNHGuiData data, int width) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        size(width, 20);
        coverChildrenHeight(20);
        final int HEAD_SIZE = 8;
        final int GAP = 4;
        final int CHECK_W = net.minecraft.client.Minecraft.getMinecraft().fontRenderer.getStringWidth("[x]");
        final int NAME_W = width - GAP - CHECK_W - GAP - HEAD_SIZE - GAP;

        for (PlayerEntry player : resolveAvailablePlayers()) {
            boolean assigned = task.assignees.stream()
                .anyMatch(
                    ap -> ap.playerId()
                        .equals(player.id()));

            var checkMark = new TextWidget<>(assigned ? "[x]" : "[ ]");
            checkMark.size(CHECK_W, 20);
            checkMark.textAlign(Alignment.CenterLeft);
            checkMark.marginLeft(GAP);

            PlayerHeadWidget head = new PlayerHeadWidget(player.name());
            head.size(HEAD_SIZE, HEAD_SIZE)
                .marginTop(6)
                .marginLeft(GAP);

            var nameLabel = new TextWidget<>(player.name());
            nameLabel.size(NAME_W, 20);
            nameLabel.textAlign(Alignment.CenterLeft);
            nameLabel.marginLeft(GAP);

            Flow row = Flow.row()
                .size(width, 20);
            row.child(checkMark);
            row.child(head);
            row.child(nameLabel);

            child(
                new ButtonWidget<>().size(width, 20)
                    .child(row)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        if (task.assignees.stream()
                            .anyMatch(
                                ap -> ap.playerId()
                                    .equals(player.id()))) {
                            task.assignees.removeIf(
                                ap -> ap.playerId()
                                    .equals(player.id()));
                        } else {
                            task.assignees.add(new AssignedPlayer(player.id(), System.currentTimeMillis()));
                        }
                        TaskNHNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
                        TaskNHGui.open(data);
                        return true;
                    }));
        }
    }

    private static List<PlayerEntry> resolveAvailablePlayers() {
        List<PlayerEntry> members = TaskNHClientCache.getTeamMembers();
        if (!members.isEmpty()) return members;
        // Fallback before first sync: show only local player
        EntityPlayer local = Minecraft.getMinecraft().thePlayer;
        return java.util.Collections.singletonList(
            new PlayerEntry(
                local.getGameProfile()
                    .getId(),
                local.getCommandSenderName()));
    }
}
