package com.eldrinn.tasknh.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.config.PinnedTasksConfig;
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
    public static final int ROW_WIDTH = LEFT_WIDTH - 2 * TaskNHGui.PADDING - SCROLLBAR_W;
    public static final int ROW_HEIGHT = 20;
    private static final int ICON_W = 20;
    private static final int PIN_BTN_W = 20;
    private static final int FOLD_BTN_W = 20;
    /** MUI ships this arrow as a texture but has no GuiTextures constant for it. */
    private static final UITexture ARROW_RIGHT = UITexture.fullImage(ModularUI.ID, "gui/icons/arrow_right");
    /** Width of the reorder column, holding the up and down buttons stacked on top of each other. */
    private static final int MOVE_BTN_W = 16;
    /** Left offset of a child task row, so nesting is visible in the flat list. */
    private static final int INDENT_W = 16;

    public TaskRowWidget(Task task, TaskNHGuiData data, boolean isChild) {
        this(task, data, isChild, 0, 0, null, null);
    }

    /**
     * {@code childCount} and {@code doneChildren} count this row's subtasks, all and done ones, for the fold button.
     */
    public TaskRowWidget(Task task, TaskNHGuiData data, boolean isChild, int childCount, int doneChildren) {
        this(task, data, isChild, childCount, doneChildren, null, null);
    }

    /**
     * @param moveUp   swaps this task with the one above it, or null when it is already first
     * @param moveDown swaps this task with the one below it, or null when it is already last
     */
    public TaskRowWidget(Task task, TaskNHGuiData data, boolean isChild, @Nullable Runnable moveUp,
        @Nullable Runnable moveDown) {
        this(task, data, isChild, 0, 0, moveUp, moveDown);
    }

    private TaskRowWidget(Task task, TaskNHGuiData data, boolean isChild, int childCount, int doneChildren,
        @Nullable Runnable moveUp, @Nullable Runnable moveDown) {
        super(com.cleanroommc.modularui.api.GuiAxis.X);
        final int indent = isChild ? INDENT_W : 0;
        size(ROW_WIDTH, 20);
        // Only subtasks carry the reorder buttons: root tasks are dragged instead.
        final int moveColumn = isChild ? MOVE_BTN_W : 0;
        // Subtasks can't be pinned: the parent covers that, so the freed width goes to the title.
        final int SELECT_BTN_W = (isChild ? ROW_WIDTH - indent : ROW_WIDTH - PIN_BTN_W) - moveColumn;
        // Spacer instead of a margin: the list layout ignores the margin and would shift the pin button.
        if (indent > 0) {
            var spacer = new TextWidget<>("");
            spacer.size(indent, 20);
            child(spacer);
        }

        // A plain button, not a ToggleButton: the latter stops the click from travelling further, which would keep
        // the row from being picked up for a drag. Selecting the task is handled by TaskBlockItem, since a press here
        // always starts a drag and a release without movement never reaches a click handler.
        ButtonWidget<?> selectBtn = new ButtonWidget<>();
        selectBtn.size(SELECT_BTN_W, 20);
        if (task.id.equals(data.selectedTaskId)) {
            selectBtn.background(new Rectangle().setColor(ColorUtils.backgroundRowSelected.getColor()));
        }
        // The row keeps its full width, but the content stops short of the fold button so no text
        // ends up underneath it. A row without that button gives the whole width to the title.
        final int CONTENT_W = SELECT_BTN_W - (childCount > 0 ? FOLD_BTN_W : 0);
        selectBtn.child(buildRowContent(task, SELECT_BTN_W, CONTENT_W));

        child(selectBtn);
        if (isChild) {
            child(
                Flow.column()
                    .size(MOVE_BTN_W, 20)
                    .child(moveButton(GuiTextures.MOVE_UP, moveUp))
                    .child(moveButton(GuiTextures.MOVE_DOWN, moveDown)));
            return;
        }

        if (childCount > 0) {
            boolean hasDone = doneChildren > 0;
            // Hiding the done subtasks only differs from the other two states when some are done and some are not.
            // Otherwise it hides nothing or everything, so the button shows and cycles it as that state.
            boolean mixed = hasDone && doneChildren < childCount;
            PinnedTasksConfig.Fold fold = TaskNHClientCache.getPinConfig()
                .getFold(task.id);
            if (fold == PinnedTasksConfig.Fold.HIDE_DONE && !mixed) {
                fold = hasDone ? PinnedTasksConfig.Fold.HIDE_ALL : PinnedTasksConfig.Fold.SHOW_ALL;
            }
            final PinnedTasksConfig.Fold next = nextFold(fold, mixed);
            ButtonWidget<?> foldBtn = new ButtonWidget<>();
            foldBtn.size(FOLD_BTN_W, 20);
            // Sits left of the pin button. Flow skips children whose position on its axis is set,
            // so the button takes no width from the row itself.
            foldBtn.right(PIN_BTN_W);
            foldBtn.overlay(foldIcon(fold));
            foldBtn.addTooltipLine(
                net.minecraft.util.StatCollector.translateToLocal(
                    "tasknh.gui.row.fold." + fold.name()
                        .toLowerCase(java.util.Locale.ROOT)));
            if (hasDone) {
                foldBtn.addTooltipLine(
                    String.format(
                        net.minecraft.util.StatCollector.translateToLocal("tasknh.gui.row.done_children"),
                        doneChildren));
            }
            foldBtn.onMousePressed(btn -> {
                if (btn != 0) return false;
                TaskNHClientCache.getPinConfig()
                    .setFold(task.id, next);
                TaskNHGui.open(data);
                return true;
            });
            child(foldBtn);
        }

        boolean pinned = TaskNHClientCache.isPinned(task.id);
        boolean canPin = TaskNHClientCache.canPin();
        IDrawable pinIcon;
        if (pinned) {
            pinIcon = GuiTextures.FAVORITE.withColorOverride(ColorUtils.iconPinActive.getColor());
        } else if (canPin) {
            pinIcon = GuiTextures.FAVORITE_OUTLINE;
        } else {
            pinIcon = GuiTextures.FAVORITE_OUTLINE.withColorOverride(ColorUtils.iconPinInactive.getColor());
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

        child(pinBtn);
    }

    /**
     * Unfolded, then done subtasks hidden, then all hidden. The middle step is skipped unless {@code mixed}: some
     * subtasks done and some not.
     */
    private static PinnedTasksConfig.Fold nextFold(PinnedTasksConfig.Fold fold, boolean mixed) {
        switch (fold) {
            case SHOW_ALL:
                return mixed ? PinnedTasksConfig.Fold.HIDE_DONE : PinnedTasksConfig.Fold.HIDE_ALL;
            case HIDE_DONE:
                return PinnedTasksConfig.Fold.HIDE_ALL;
            default:
                return PinnedTasksConfig.Fold.SHOW_ALL;
        }
    }

    /** Down arrow when unfolded, the same arrow greyed out when done subtasks are hidden, right arrow when all are. */
    private static IDrawable foldIcon(PinnedTasksConfig.Fold fold) {
        UITexture texture;
        switch (fold) {
            case SHOW_ALL:
                texture = GuiTextures.ARROW_DOWN;
                break;
            case HIDE_DONE:
                texture = GuiTextures.ARROW_DOWN.withColorOverride(ColorUtils.iconPinInactive.getColor());
                break;
            default:
                texture = ARROW_RIGHT;
        }
        // The arrow textures are 10x10; drawn at their own size instead of stretched over the button.
        return texture.asIcon()
            .size(10);
    }

    /** An edge row keeps the button so the column width stays the same, greyed out and inert. */
    private static ButtonWidget<?> moveButton(UITexture icon, @Nullable Runnable action) {
        ButtonWidget<?> btn = new ButtonWidget<>();
        btn.size(MOVE_BTN_W, 10);
        btn.overlay(action == null ? icon.withColorOverride(ColorUtils.iconPinInactive.getColor()) : icon);
        btn.onMousePressed(mouseButton -> {
            if (mouseButton != 0 || action == null) return false;
            action.run();
            return true;
        });
        return btn;
    }

    private static final int TEXT_PAD = 4;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_GAP = 2;

    private static Flow buildRowContent(Task task, int SELECT_BTN_W, int CONTENT_W) {
        ItemStack stack = task.iconItem;
        Flow row = Flow.row()
            .size(SELECT_BTN_W, 20);
        int used = 0;

        if (stack != null) {
            row.child(new InlineIconWidget(stack).size(ICON_W, 20));
            used += ICON_W;
        }

        // A done subtask is struck through in place instead of moving to the Done tab.
        String title = truncate(task.title);
        if (task.parentId != null && task.status == com.eldrinn.tasknh.data.TaskStatus.DONE) {
            title = net.minecraft.util.EnumChatFormatting.STRIKETHROUGH + title;
        }
        int leftPad = stack == null ? TEXT_PAD : 0;
        int assigneeW = assigneeBlockWidth(task);
        int maxTitleW = CONTENT_W - used - leftPad - assigneeW;
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
