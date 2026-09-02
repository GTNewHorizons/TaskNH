package com.eldrinn.tasknh.gui.widget;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ListWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A ListWidget that survives the full GUI rebuild every edit triggers. The scroll offset and the
 * content size are kept in a {@link Memory} owned by the GUI data and written back on every frame.
 * The offset is restored once the new children are laid out; the content size is seeded before the
 * first resize so the scrollbar is already known to be active and the content is not laid out one
 * frame too wide.
 */
@SideOnly(Side.CLIENT)
public class ScrollMemoryList extends ListWidget<IWidget, ScrollMemoryList> {

    /** Scroll state of one list, kept across rebuilds. */
    public static class Memory {

        private int scroll = 0;
        private int size = 0;

        public void reset() {
            this.scroll = 0;
            this.size = 0;
        }
    }

    private final Memory memory;
    private boolean restored = false;

    public ScrollMemoryList(Memory memory, int scrollbarWidth) {
        this.memory = memory;
        scrollDirection(new VerticalScrollData(false, scrollbarWidth));
        getScrollData().setScrollSize(memory.size);
    }

    @Override
    public boolean postLayoutWidgets() {
        boolean done = super.postLayoutWidgets();
        if (done && !this.restored) {
            this.restored = true;
            getScrollData().scrollTo(getScrollArea(), this.memory.scroll);
        }
        return done;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        this.memory.scroll = getScrollData().getScroll();
        this.memory.size = getScrollData().getScrollSize();
    }
}
