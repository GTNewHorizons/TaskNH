package com.eldrinn.tasknh.gui.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.HoveredWidgetList;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.SortableListWidget;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.ColorUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One draggable block of the task list: a root task with its subtasks.
 * <p>
 * The block itself is never disabled by the search, because dragging disables it through the very same flag and the
 * filter would keep switching it back on every tick. The rows inside carry the filter and collapse out of the block,
 * which leaves a block with no match at zero height.
 */
@SideOnly(Side.CLIENT)
public class TaskBlockItem extends SortableListWidget.Item<Task> {

    /**
     * The rows the block currently shows, top one first, so a click can be resolved to the row it landed on. Read at
     * click time: the search hides subtask rows, and hidden ones are collapsed out of the block.
     */
    private final Supplier<List<Task>> visibleRows;
    private final Consumer<Task> onClick;
    private final Runnable onDropped;
    /** Makes the dragged task a subtask of the given root. */
    private final Consumer<Task> onNest;
    private final boolean hasSubtasks;
    /** How far the cursor may travel and still count as a click rather than a drag. */
    private static final int CLICK_SLOP = 2;
    // Only one block is dragged at a time, so the drag state below is static: the other blocks read it to draw their
    // zones and frame.
    /** The block being dragged, or null when none is. */
    @Nullable
    private static TaskBlockItem dragging;
    /**
     * The block whose nesting zone the cursor is over, whether or not the dragged block may nest there. Each block
     * checks its own zone while drawing, see drawForeground, so the drag reads what the last frame found.
     */
    @Nullable
    private static TaskBlockItem zoneBlock;
    private int dragStartX;
    private int dragStartY;
    /** Cursor offset inside the block when the drag began, so the floating copy keeps its grip point. */
    private int relativeClickX;
    private int relativeClickY;
    /** Where the cursor sat at the last swap, the anchor the travel distance is measured from. */
    private int lastSwapY;

    public TaskBlockItem(Task task, boolean hasSubtasks, Supplier<List<Task>> visibleRows, Consumer<Task> onClick,
        Runnable onDropped, Consumer<Task> onNest) {
        super(task);
        this.hasSubtasks = hasSubtasks;
        this.visibleRows = visibleRows;
        this.onClick = onClick;
        this.onDropped = onDropped;
        this.onNest = onNest;
    }

    /**
     * Hides the rows from the cursor while a block is being dragged, so their buttons stop lighting up under it
     * and the block itself is what the drag lands on, whichever of its rows the cursor happens to be over.
     */
    @Override
    public void getWidgetsAt(IViewportStack stack, HoveredWidgetList widgets, int x, int y) {
        if (getContext().hasDraggable()) return;
        super.getWidgetsAt(stack, widgets, x, y);
    }

    /** Remembers where the gesture began, so a release can tell a click from a dropped drag. */
    @Override
    public boolean onDragStart(int button) {
        boolean started = super.onDragStart(button);
        this.dragStartX = getContext().getAbsMouseX();
        this.dragStartY = getContext().getAbsMouseY();
        this.lastSwapY = this.dragStartY;
        dragging = this;
        zoneBlock = null;
        Area moving = getMovingArea();
        if (moving != null) {
            this.relativeClickX = this.dragStartX - moving.x;
            this.relativeClickY = this.dragStartY - moving.y;
        }
        return started;
    }

    /**
     * Carries the floating copy and swaps blocks, in place of the stock handler. That one swaps as soon as the cursor
     * touches another block, and blocks here differ in height: swapping with a tall one slides it right back under the
     * cursor, which swaps it again three ticks later. A swap needs the cursor to travel past the middle of the block
     * it displaces first, so passing over a tall block no longer makes it jump.
     */
    @Override
    public void onDrag(int mouseButton, long timeSinceLastClick) {
        Area moving = getMovingArea();
        if (moving != null) {
            moving.x = getContext().getAbsMouseX() - this.relativeClickX;
            moving.y = getContext().getAbsMouseY() - this.relativeClickY;
        }
        if (!(getParent() instanceof SortableListWidget<?>listWidget)) return;
        // Over a zone the blocks stay put, or the target would slide away before the drop.
        if (zoneBlock != null) return;
        for (LocatedWidget hovering : getPanel().getAllHoveringList(false)) {
            if (!(hovering.getElement() instanceof TaskBlockItem other) || other == this) continue;
            int travelled = Math.abs(getContext().getAbsMouseY() - this.lastSwapY);
            if (travelled < Math.max(TaskRowWidget.ROW_HEIGHT, other.getArea().height / 2)) return;
            listWidget.moveTo(getIndex(), other.getIndex());
            this.lastSwapY = getContext().getAbsMouseY();
            return;
        }
    }

    /**
     * A press on a row starts a drag before any click handler can run, and a release without movement cancels that
     * drag instead of tapping the row. So a cancelled drag is what a plain click looks like from here, and the row is
     * resolved from where the cursor sits inside the block.
     */
    @Override
    public void onDragEnd(boolean successful) {
        TaskBlockItem target = nestTarget();
        dragging = null;
        zoneBlock = null;
        if (target != null) {
            this.onNest.accept(target.getWidgetValue());
            return;
        }
        if (successful) {
            // The list swaps blocks all the way through the gesture, so the order is written back once,
            // here, instead of once per intermediate swap.
            this.onDropped.run();
            return;
        }
        // A block dropped outside the list ends the same way a click does, so a cursor that travelled
        // means the player meant to drag and dropped it somewhere that took nothing.
        if (hasMoved()) return;
        int localY = getContext().getAbsMouseY() - getContext().transformY(0, 0);
        int index = localY / TaskRowWidget.ROW_HEIGHT;
        List<Task> rows = this.visibleRows.get();
        if (index >= 0 && index < rows.size()) {
            this.onClick.accept(rows.get(index));
        }
    }

    /** Draws the floating copy, plus a line above the window that explains or confirms the nesting gesture. */
    @Override
    public void drawMovingState(ModularGuiContext context, float partialTicks) {
        super.drawMovingState(context, partialTicks);
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int maxW = getPanel().getArea().width - 4;
        TaskBlockItem nestTarget = nestTarget();
        String hint;
        int color;
        if (nestTarget != null) {
            String key = "tasknh.gui.list.nest_target";
            int titleW = maxW - fr.getStringWidth(StatCollector.translateToLocalFormatted(key, ""));
            hint = StatCollector.translateToLocalFormatted(
                key,
                fr.trimStringToWidth(nestTarget.getWidgetValue().title, Math.max(0, titleW)));
            color = ColorUtils.accentGold.getColor();
        } else if (zoneBlock != null && this.hasSubtasks) {
            hint = StatCollector.translateToLocal("tasknh.gui.list.nest_blocked");
            color = ColorUtils.iconRemove.getColor();
        } else if (hasMoved()) {
            hint = StatCollector.translateToLocal("tasknh.gui.list.nest_hint");
            color = ColorUtils.textWhite.getColor();
        } else {
            return;
        }
        hint = fr.trimStringToWidth(hint, maxW);
        // Fixed above the window rather than under the floating copy, which carried it around the screen. No offset is
        // applied here, so screen coordinates are used as they are.
        Area panel = getPanel().getArea();
        int boxW = fr.getStringWidth(hint) + 4;
        int x = panel.x + (panel.width - boxW) / 2;
        int y = Math.max(2, panel.y - fr.FONT_HEIGHT - 6);
        GuiDraw.drawRect(x, y, boxW, fr.FONT_HEIGHT + 3, ColorUtils.backgroundPanel.getColor());
        fr.drawStringWithShadow(hint, x + 2, y + 2, color);
    }

    /**
     * While another block is dragged, marks the part of this task's row that nests it here, and frames the whole block
     * once the cursor is over that part.
     */
    @Override
    public void drawForeground(ModularGuiContext context) {
        super.drawForeground(context);
        // The foreground, unlike the overlay, comes after the rows, which would otherwise paint over the zone. It is
        // drawn in screen coordinates, so the block's origin is added by hand.
        int x0 = context.transformX(0, 0);
        int y0 = context.transformY(0, 0);
        TaskBlockItem dragged = dragging;
        boolean active = dragged != null && dragged != this
            && dragged.hasMoved()
            && takesSubtasks()
            && canBeSeen(context);
        boolean over = active && inZone(context.getAbsMouseX() - x0, context.getAbsMouseY() - y0);
        if (over) {
            zoneBlock = this;
        } else if (zoneBlock == this) {
            zoneBlock = null;
        }
        // A task with subtasks can't be nested, so its drag shows no zones; the line above the window says why.
        if (!active || dragged.hasSubtasks) return;
        int gold = ColorUtils.accentGold.getColor() & 0xFFFFFF;
        int w = getArea().width;
        int zoneW = zoneWidth();
        GuiDraw.drawRect(x0 + w - zoneW, y0, zoneW, TaskRowWidget.ROW_HEIGHT, (over ? 0x80000000 : 0x40000000) | gold);
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String mark = "»";
        fr.drawStringWithShadow(
            mark,
            x0 + w - zoneW + (zoneW - fr.getStringWidth(mark)) / 2,
            y0 + (TaskRowWidget.ROW_HEIGHT - fr.FONT_HEIGHT) / 2 + 1,
            0xFF000000 | gold);
        if (!over) return;
        int frame = 0xFF000000 | gold;
        int h = getArea().height;
        GuiDraw.drawRect(x0, y0, w, 1, frame);
        GuiDraw.drawRect(x0, y0 + h - 1, w, 1, frame);
        GuiDraw.drawRect(x0, y0, 1, h, frame);
        GuiDraw.drawRect(x0 + w - 1, y0, 1, h, frame);
    }

    /** The block the dragged one would nest under if dropped now, or null. */
    @Nullable
    private static TaskBlockItem nestTarget() {
        return dragging == null || dragging.hasSubtasks ? null : zoneBlock;
    }

    /** Whether a task may be nested under this block's task: it has to be a root. */
    private boolean takesSubtasks() {
        // An orphan shows as a root but still has a parent id, so the server would refuse it.
        return getWidgetValue().parentId == null;
    }

    /** Width of the nesting zone, the right part of the task's own row. */
    private int zoneWidth() {
        return getArea().width / 3;
    }

    /** Whether a point in this block's coordinates lies in its nesting zone. */
    private boolean inZone(int x, int y) {
        int w = getArea().width;
        return y >= 0 && y < TaskRowWidget.ROW_HEIGHT && x >= w - zoneWidth() && x < w;
    }

    /** False for a plain click, which also starts a drag, so the hint doesn't flash on every click. */
    private boolean hasMoved() {
        return Math.abs(getContext().getAbsMouseX() - this.dragStartX) > CLICK_SLOP
            || Math.abs(getContext().getAbsMouseY() - this.dragStartY) > CLICK_SLOP;
    }
}
