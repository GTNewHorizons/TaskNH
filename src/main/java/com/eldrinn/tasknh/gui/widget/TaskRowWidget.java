package com.eldrinn.tasknh.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskRowWidget extends Flow {

    private static final int LEFT_WIDTH = TaskNHGui.LEFT_WIDTH;
    public static final int SCROLLBAR_W = 4;
    private static final int ROW_WIDTH = LEFT_WIDTH - 2 * TaskNHGui.PADDING - SCROLLBAR_W;
    private static final int ICON_W = 20;
    private static final int PIN_BTN_W = 20;
    private static final int SELECT_BTN_W = ROW_WIDTH - PIN_BTN_W;

    public TaskRowWidget(Task task, TaskNHGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.X);
        size(ROW_WIDTH, 20);

        ToggleButton selectBtn = new ToggleButton();
        selectBtn.size(SELECT_BTN_W, 20);
        selectBtn.value(new BoolValue.Dynamic(() -> task.id.equals(data.selectedTaskId), selected -> {
            if (selected) {
                data.selectTask(task.id);
                TaskNHGui.open(data);
            }
        }));
        selectBtn.child(false, buildRowContent(task));
        selectBtn.child(true, buildRowContent(task));

        boolean pinned = TaskNHClientCache.isPinned(task.id);
        boolean canPin = TaskNHClientCache.canPin();
        IDrawable pinIcon;
        if (pinned) {
            pinIcon = GuiTextures.FAVORITE.withColorOverride(ColorUtils.PIN_ACTIVE.getColor());
        } else if (canPin) {
            pinIcon = GuiTextures.FAVORITE_OUTLINE;
        } else {
            pinIcon = GuiTextures.FAVORITE_OUTLINE.withColorOverride(ColorUtils.PIN_INACTIVE.getColor());
        }
        ButtonWidget<?> pinBtn = new ButtonWidget<>();
        pinBtn.size(PIN_BTN_W, 20);
        pinBtn.overlay(pinIcon);
        pinBtn.onMousePressed(btn -> {
            if (btn != 0) return false;
            if (TaskNHClientCache.isPinned(task.id)) {
                TaskNHClientCache.unpin(task.id);
            } else {
                TaskNHClientCache.pin(task.id);
            }
            TaskNHGui.open(data);
            return true;
        });

        child(selectBtn);
        child(pinBtn);
    }

    private static final int TEXT_PAD = 4;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_GAP = 2;

    private static Flow buildRowContent(Task task) {
        ItemStack stack = IconSlotWidget.parseIconItem(task.iconItem);
        Flow row = Flow.row()
            .size(SELECT_BTN_W, 20);
        int used = 0;

        if (stack != null) {
            row.child(new InlineIconWidget(stack).size(ICON_W, 20));
            used += ICON_W;
        }

        String title = truncate(task.title);
        int leftPad = stack == null ? TEXT_PAD : 0;
        int assigneeW = assigneeBlockWidth(task);
        int maxTitleW = SELECT_BTN_W - used - leftPad - assigneeW;
        int titlePixelW = Minecraft.getMinecraft().fontRenderer.getStringWidth(title) + 4;
        var titleLabel = new TextWidget<>(title);
        titleLabel.textAlign(Alignment.CenterLeft);
        titleLabel.marginLeft(leftPad);
        titleLabel.size(Math.min(titlePixelW, maxTitleW), 20);
        row.child(titleLabel);

        // assignee heads + names
        int shown = 0;
        for (AssignedPlayer ap : task.assignees) {
            if (shown >= 2) {
                String more = String.format(
                    net.minecraft.util.StatCollector.translateToLocal("tasknh.gui.row.more"),
                    task.assignees.size() - 2);
                var moreLabel = new TextWidget<>(more);
                moreLabel.size(30, 20);
                moreLabel.textAlign(Alignment.CenterLeft);
                row.child(moreLabel);
                break;
            }
            String name = resolveName(ap.playerId());
            if (name != null) {
                row.child(
                    new PlayerHeadWidget(name).size(HEAD_SIZE, HEAD_SIZE)
                        .marginTop(6)
                        .marginLeft(HEAD_GAP));
                var nameLabel = new TextWidget<>("[" + name + "]");
                nameLabel.size(nameTextWidth(name), 20);
                nameLabel.textAlign(Alignment.CenterLeft);
                nameLabel.marginLeft(HEAD_GAP);
                row.child(nameLabel);
            }
            shown++;
        }

        return row;
    }

    private static int assigneeBlockWidth(Task task) {
        if (task.assignees.isEmpty()) return 0;
        int w = 0;
        int shown = 0;
        for (AssignedPlayer ap : task.assignees) {
            if (shown >= 2) {
                w += 30;
                break;
            }
            String name = resolveName(ap.playerId());
            if (name != null) {
                w += HEAD_GAP + HEAD_SIZE + nameTextWidth(name);
            }
            shown++;
        }
        return w;
    }

    private static int nameTextWidth(String name) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth("[" + name + "]") + 2;
    }

    private static @Nullable String resolveName(UUID uuid) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().theWorld
                .getPlayerEntityByName(info.name);
            if (player != null && uuid.equals(
                player.getGameProfile()
                    .getId()))
                return info.name;
        }
        return null;
    }

    private static String truncate(String s) {
        return s.length() <= 22 ? s : s.substring(0, 21) + "~";
    }

    @SideOnly(Side.CLIENT)
    private static class InlineIconWidget extends Widget<InlineIconWidget> {

        private final ItemStack stack;

        InlineIconWidget(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
            return theme.getFallback();
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            int pad = 2;
            GuiDraw.drawItem(
                stack,
                pad,
                pad,
                getArea().width - 2 * pad,
                getArea().height - 2 * pad,
                context.getCurrentDrawingZ());
        }
    }
}
