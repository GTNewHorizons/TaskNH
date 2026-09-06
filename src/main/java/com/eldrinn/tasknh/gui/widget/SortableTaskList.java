package com.eldrinn.tasknh.gui.widget;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.SortableListWidget;
import com.eldrinn.tasknh.data.Task;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The task list, sorted by dragging its rows. Keeps the scroll offset across the full GUI rebuild every edit
 * triggers, the same way {@link ScrollMemoryList} does, since the two lists cannot share a base class.
 */
@SideOnly(Side.CLIENT)
public class SortableTaskList extends SortableListWidget<Task> {

    private final ScrollMemoryList.Memory memory;
    private boolean restored = false;

    public SortableTaskList(ScrollMemoryList.Memory memory, int scrollbarWidth) {
        this.memory = memory;
        // The sortable list turns this off for its move animation, but the search hides rows by disabling them and
        // needs them out of the layout, not left behind as gaps.
        collapseDisabledChild(true);
        // Rows are narrower than the list by the scrollbar width. Centering, the default, would
        // split that gap and move the whole list sideways whenever the scrollbar turns on or off.
        crossAxisAlignment(com.cleanroommc.modularui.utils.Alignment.CrossAxis.START);
        scrollDirection(new VerticalScrollData(false, scrollbarWidth));
        getScrollData().setScrollSize(memory.getSize());
    }

    @Override
    public boolean postLayoutWidgets() {
        boolean done = super.postLayoutWidgets();
        if (done && !this.restored) {
            this.restored = true;
            getScrollData().scrollTo(getScrollArea(), this.memory.getScroll());
        }
        return done;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        this.memory.store(getScrollData().getScroll(), getScrollData().getScrollSize());
    }
}
