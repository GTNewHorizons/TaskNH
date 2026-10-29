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
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
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
    /** How far right of where the drag began the cursor must be for the drop to nest instead of reorder. */
    private static final int NEST_SHIFT = 16;
    /** The block the dragged one would nest under if dropped now. Static, since only one block is dragged at a time. */
    @Nullable
    private static TaskBlockItem nestTarget;
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
        nestTarget = null;
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
        nestTarget = null;
        // While nesting the blocks stay put, or the one under the cursor would slide away before it is dropped on.
        if (isNesting()) {
            if (this.hasSubtasks) return;
            for (LocatedWidget hovering : getPanel().getAllHoveringList(false)) {
                // An orphan shows as a root but still has a parent id, so the server would refuse it.
                if (hovering.getElement() instanceof TaskBlockItem other && other != this
                    && other.getWidgetValue().parentId == null) {
                    nestTarget = other;
                    return;
                }
            }
            return;
        }
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
        TaskBlockItem target = nestTarget;
        nestTarget = null;
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
        String hint;
        int color;
        if (nestTarget != null) {
            String key = "tasknh.gui.list.nest_target";
            int titleW = maxW - fr.getStringWidth(StatCollector.translateToLocalFormatted(key, ""));
            hint = StatCollector.translateToLocalFormatted(
                key,
                fr.trimStringToWidth(nestTarget.getWidgetValue().title, Math.max(0, titleW)));
            color = ColorUtils.accentGold.getColor();
        } else if (isNesting() && this.hasSubtasks) {
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

    /** Frames the block the dragged one would nest under. */
    @Override
    public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.drawOverlay(context, widgetTheme);
        if (nestTarget != this) return;
        int color = 0xFF000000 | ColorUtils.accentGold.getColor();
        int w = getArea().width;
        int h = getArea().height;
        GuiDraw.drawRect(0, 0, w, 1, color);
        GuiDraw.drawRect(0, h - 1, w, 1, color);
        GuiDraw.drawRect(0, 0, 1, h, color);
        GuiDraw.drawRect(w - 1, 0, 1, h, color);
    }

    private boolean isNesting() {
        return getContext().getAbsMouseX() - this.dragStartX >= NEST_SHIFT;
    }

    /** False for a plain click, which also starts a drag, so the hint doesn't flash on every click. */
    private boolean hasMoved() {
        return Math.abs(getContext().getAbsMouseX() - this.dragStartX) > CLICK_SLOP
            || Math.abs(getContext().getAbsMouseY() - this.dragStartY) > CLICK_SLOP;
    }
}
