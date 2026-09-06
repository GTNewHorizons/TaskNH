package com.eldrinn.tasknh.gui.widget;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.widgets.SortableListWidget;
import com.eldrinn.tasknh.data.Task;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One draggable block of the task list: a root task with its subtasks.
 * <p>
 * The search filter cannot go through {@code setEnabledIf}, because dragging hides the original row with the very same
 * enabled flag and the filter would keep switching it back on every tick, leaving the row behind its own dragged copy.
 * The filter gets its own condition here instead, so both can hide the block independently.
 */
@SideOnly(Side.CLIENT)
public class TaskBlockItem extends SortableListWidget.Item<Task> {

    private final BooleanSupplier matchesSearch;
    /**
     * The rows the block currently shows, top one first, so a click can be resolved to the row it landed on. Read at
     * click time: the search hides subtask rows, and hidden ones are collapsed out of the block.
     */
    private final Supplier<List<Task>> visibleRows;
    private final Consumer<Task> onClick;

    public TaskBlockItem(Task task, Supplier<List<Task>> visibleRows, BooleanSupplier matchesSearch,
        Consumer<Task> onClick) {
        super(task);
        this.visibleRows = visibleRows;
        this.matchesSearch = matchesSearch;
        this.onClick = onClick;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && this.matchesSearch.getAsBoolean();
    }

    /**
     * A press on a row starts a drag before any click handler can run, and a release without movement cancels that
     * drag instead of tapping the row. So a cancelled drag is what a plain click looks like from here, and the row is
     * resolved from where the cursor sits inside the block.
     */
    @Override
    public void onDragEnd(boolean successful) {
        if (successful) return;
        int localY = getContext().getAbsMouseY() - getContext().transformY(0, 0);
        int index = localY / TaskRowWidget.ROW_HEIGHT;
        List<Task> rows = this.visibleRows.get();
        if (index >= 0 && index < rows.size()) {
            this.onClick.accept(rows.get(index));
        }
    }
}
