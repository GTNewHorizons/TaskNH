package com.eldrinn.tasknh.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskListWidget extends Flow {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TaskListWidget(TaskNHGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        final int HEIGHT = TaskNHGui.getHeight();
        size(TaskNHGui.LEFT_WIDTH, HEIGHT);
        padding(TaskNHGui.PADDING);

        final int P = TaskNHGui.PADDING;
        final int W = TaskNHGui.LEFT_WIDTH - 2 * P;
        final int H = HEIGHT - 2 * P;

        // Tabs — each tab takes exactly 1/3 of the available width
        final int TAB_W = W / 3;
        Collection<Task> allTasks = TaskNHClientCache.getAll();
        child(
            Flow.row()
                .size(W, 24)
                .child(
                    tabButton(tabLabel("tasknh.gui.tab.open", TaskStatus.OPEN, allTasks), TaskStatus.OPEN, data, TAB_W))
                .child(
                    tabButton(
                        tabLabel("tasknh.gui.tab.in_progress", TaskStatus.IN_PROGRESS, allTasks),
                        TaskStatus.IN_PROGRESS,
                        data,
                        TAB_W))
                .child(
                    tabButton(
                        tabLabel("tasknh.gui.tab.done", TaskStatus.DONE, allTasks),
                        TaskStatus.DONE,
                        data,
                        W - TAB_W * 2)));

        // Search: icon button toggles field; live search on keystroke
        final int SEARCH_BTN_W = 20;
        Flow searchRow = Flow.row()
            .size(W, 20)
            .marginTop(P);
        searchRow.child(
            new ButtonWidget<>().size(SEARCH_BTN_W, 20)
                .overlay(GuiTextures.SEARCH)
                .addTooltipLine(StatCollector.translateToLocal("tasknh.gui.search.tooltip"))
                .onMousePressed(btn -> {
                    if (btn != 0) return false;
                    data.searchExpanded = !data.searchExpanded;
                    if (!data.searchExpanded) {
                        data.searchQuery = "";
                    }
                    TaskNHGui.open(data);
                    return true;
                }));
        if (data.searchExpanded) {
            PlainTextField searchField = new PlainTextField();
            searchField.size(W - SEARCH_BTN_W, 20);
            searchField.setTextColor(ColorUtils.textWhite.getColor());
            searchField.autoUpdateOnChange(true);
            // No rebuild here: rebuilding on every keystroke dropped the field's focus, so only one
            // character made it in per click. The rows filter themselves instead, see below.
            searchField.value(new StringValue.Dynamic(() -> data.searchQuery, val -> data.searchQuery = val));
            searchRow.child(searchField);
        }
        child(searchRow);

        // Task list filtered by active tab and search query
        SortableTaskList list = new SortableTaskList(data.listScroll, TaskRowWidget.SCROLLBAR_W);
        list.size(W, H - 24 - P - 20 - P - 28);
        list.marginTop(P);
        // Rows for the whole tab are built once; the search query only enables and disables them,
        // and the list collapses the disabled ones out of the layout. That keeps typing free of
        // rebuilds, which would take the search field's focus with them.
        // Roots of the active tab in their manual order. An orphaned subtask (parent gone) would otherwise vanish,
        // so it joins them and is shown as a root.
        List<Task> roots = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.status != data.activeTab) continue;
            if (task.parentId == null || TaskNHClientCache.get(task.parentId) == null) roots.add(task);
        }
        roots.sort(ORDER);
        for (Task task : roots) {
            // Children follow their parent, indented. Nesting is one level deep. The list is
            // taken once here rather than per tick, since any change to it rebuilds the rows.
            List<Task> children = new ArrayList<>();
            for (Task child : allTasks) {
                // A subtask stays under its parent whatever its status; only search filters it.
                if (task.id.equals(child.parentId)) children.add(child);
            }
            children.sort(ORDER);
            // A parent and its subtasks are dragged as one block, so nesting survives a reorder.
            Flow block = Flow.column()
                .size(TaskRowWidget.ROW_WIDTH, 20 * (1 + children.size()));
            block.collapseDisabledChild();
            block.child(new TaskRowWidget(task, data, false));
            for (Task child : children) {
                TaskRowWidget row = new TaskRowWidget(
                    child,
                    data,
                    true,
                    swapAction(children, child, -1, data),
                    swapAction(children, child, 1, data));
                row.setEnabledIf(w -> matchesQuery(child, query(data)));
                block.child(row);
            }
            // A parent that doesn't match itself still shows while a child does, so the match
            // isn't left without the task it belongs to.
            TaskBlockItem item = new TaskBlockItem(task, () -> {
                List<Task> rows = new ArrayList<>();
                rows.add(task);
                for (Task child : children) {
                    if (matchesQuery(child, query(data))) rows.add(child);
                }
                return rows;
            }, () -> matchesQuery(task, query(data)) || anyMatches(children, query(data)), clicked -> {
                data.selectTask(clicked.id);
                TaskNHGui.open(data);
            });
            item.size(TaskRowWidget.ROW_WIDTH, 20 * (1 + children.size()));
            // The item draws a button frame of its own, which would sit on top of the rows.
            item.background(IDrawable.EMPTY);
            item.child(block);
            list.child(item);
        }
        // Dropping a block writes the new order back. Only the tasks that actually shifted are sent.
        list.onChange(ordered -> {
            for (int i = 0; i < ordered.size(); i++) {
                Task moved = ordered.get(i);
                if (moved.order == i) continue;
                moved.order = i;
                TaskNHNetwork.sendEditToServer(moved, new UpdateTaskPacket(moved));
            }
        });
        child(list);

        // Bottom bar: New Task + HUD settings + theme toggle
        final int ICON_BTN_W = 20;
        final int NEW_TASK_W = W - ICON_BTN_W * 2;

        var newTaskLabel = new TextWidget<>(net.minecraft.util.StatCollector.translateToLocal("tasknh.gui.new_task"));
        newTaskLabel.size(NEW_TASK_W, 20);
        newTaskLabel.textAlign(Alignment.Center);

        child(
            Flow.row()
                .size(W, 20)
                .child(
                    new ButtonWidget<>().size(NEW_TASK_W, 20)
                        .child(newTaskLabel)
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            data.enterCreateMode();
                            TaskNHGui.open(data);
                            return true;
                        }))
                .child(
                    new ButtonWidget<>().size(ICON_BTN_W, ICON_BTN_W)
                        .overlay(GuiTextures.GEAR)
                        .addTooltipLine(StatCollector.translateToLocal("tasknh.gui.hud_settings.tooltip"))
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            net.minecraft.client.Minecraft.getMinecraft()
                                .displayGuiScreen(new com.eldrinn.tasknh.hud.HudSettingsScreen());
                            return true;
                        }))
                .child(
                    new ButtonWidget<>().size(ICON_BTN_W, ICON_BTN_W)
                        .overlay(TaskNHGui.isDarkTheme() ? GuiTextures.SUN : GuiTextures.MOON)
                        .addTooltipLine(StatCollector.translateToLocal("tasknh.gui.theme_toggle.tooltip"))
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            TaskNHGui.toggleTheme();
                            TaskNHGui.open(data);
                            return true;
                        })));
    }

    private static final Comparator<Task> ORDER = Comparator.comparingInt(task -> task.order);

    /**
     * Builds the action that moves a task one slot within its siblings, or null when it sits at that end already.
     * Siblings must be sorted.
     */
    @Nullable
    private static Runnable swapAction(List<Task> siblings, Task task, int delta, TaskNHGuiData data) {
        int from = siblings.indexOf(task);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= siblings.size()) return null;
        return () -> {
            List<Task> moved = new ArrayList<>(siblings);
            moved.add(to, moved.remove(from));
            // Renumbering the whole group also repairs ties, which every task has in a world saved before ordering
            // existed. Only the tasks that actually shifted are sent.
            for (int i = 0; i < moved.size(); i++) {
                Task sibling = moved.get(i);
                if (sibling.order == i) continue;
                sibling.order = i;
                TaskNHNetwork.sendEditToServer(sibling, new UpdateTaskPacket(sibling));
            }
            TaskNHGui.open(data);
        };
    }

    private static boolean anyMatches(List<Task> tasks, String query) {
        for (Task task : tasks) {
            if (matchesQuery(task, query)) return true;
        }
        return false;
    }

    private static String query(TaskNHGuiData data) {
        return data.searchQuery.toLowerCase();
    }

    private static boolean matchesQuery(Task task, String query) {
        if (query.isEmpty()) return true;
        return task.title.toLowerCase()
            .contains(query)
            || task.description.toLowerCase()
                .contains(query);
    }

    private static String tabLabel(String key, TaskStatus status, Collection<Task> tasks) {
        long count = tasks.stream()
            .filter(t -> t.status == status && t.parentId == null)
            .count();
        return StatCollector.translateToLocal(key) + " (" + count + ")";
    }

    private static ToggleButton tabButton(String label, TaskStatus status, TaskNHGuiData data, int width) {
        var normalLabel = new TextWidget<>(label);
        normalLabel.size(width, 24);
        normalLabel.textAlign(Alignment.Center);

        var activeLabel = new TextWidget<>(label);
        activeLabel.size(width, 24);
        activeLabel.textAlign(Alignment.Center);

        return new ToggleButton().size(width, 24)
            .value(new BoolValue.Dynamic(() -> data.activeTab == status, selected -> {
                if (selected) {
                    data.activeTab = status;
                    data.clear();
                    TaskNHGui.open(data);
                }
            }))
            .child(false, normalLabel)
            .child(true, activeLabel);
    }
}
