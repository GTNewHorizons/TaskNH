package com.eldrinn.tasknh.gui.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.utils.HoveredWidgetList;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.SortableListWidget;
import com.eldrinn.tasknh.data.Task;

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
    /** How far the cursor may travel and still count as a click rather than a drag. */
    private static final int CLICK_SLOP = 2;
    private int dragStartX;
    private int dragStartY;
    /** Cursor offset inside the block when the drag began, so the floating copy keeps its grip point. */
    private int relativeClickX;
    private int relativeClickY;
    /** Where the cursor sat at the last swap, the anchor the travel distance is measured from. */
    private int lastSwapY;

    public TaskBlockItem(Task task, Supplier<List<Task>> visibleRows, Consumer<Task> onClick, Runnable onDropped) {
        super(task);
        this.visibleRows = visibleRows;
        this.onClick = onClick;
        this.onDropped = onDropped;
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
        if (successful) {
            // The list swaps blocks all the way through the gesture, so the order is written back once,
            // here, instead of once per intermediate swap.
            this.onDropped.run();
            return;
        }
        // A block dropped outside the list ends the same way a click does, so a cursor that travelled
        // means the player meant to drag and dropped it somewhere that took nothing.
        if (Math.abs(getContext().getAbsMouseX() - this.dragStartX) > CLICK_SLOP
            || Math.abs(getContext().getAbsMouseY() - this.dragStartY) > CLICK_SLOP) {
            return;
        }
        int localY = getContext().getAbsMouseY() - getContext().transformY(0, 0);
        int index = localY / TaskRowWidget.ROW_HEIGHT;
        List<Task> rows = this.visibleRows.get();
        if (index >= 0 && index < rows.size()) {
            this.onClick.accept(rows.get(index));
        }
    }
}
