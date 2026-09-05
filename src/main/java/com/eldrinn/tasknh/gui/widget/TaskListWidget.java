package com.eldrinn.tasknh.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.util.StatCollector;

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
        ScrollMemoryList list = new ScrollMemoryList(data.listScroll, TaskRowWidget.SCROLLBAR_W);
        list.size(W, H - 24 - P - 20 - P - 28);
        list.marginTop(P);
        // Rows for the whole tab are built once; the search query only enables and disables them,
        // and the list collapses the disabled ones out of the layout. That keeps typing free of
        // rebuilds, which would take the search field's focus with them.
        for (Task task : allTasks) {
            if (task.status != data.activeTab) continue;
            if (task.parentId == null) {
                // Children follow their parent, indented. Nesting is one level deep. The list is
                // taken once here rather than per tick, since any change to it rebuilds the rows.
                List<Task> children = new ArrayList<>();
                for (Task child : allTasks) {
                    // A subtask stays under its parent whatever its status; only search filters it.
                    if (task.id.equals(child.parentId)) children.add(child);
                }
                TaskRowWidget parentRow = new TaskRowWidget(task, data, false);
                // A parent that doesn't match itself still shows while a child does, so the match
                // isn't left without the task it belongs to.
                parentRow.setEnabledIf(w -> matchesQuery(task, query(data)) || anyMatches(children, query(data)));
                list.child(parentRow);
                for (Task child : children) {
                    TaskRowWidget row = new TaskRowWidget(child, data, true);
                    row.setEnabledIf(w -> matchesQuery(child, query(data)));
                    list.child(row);
                }
            } else if (TaskNHClientCache.get(task.parentId) == null) {
                // Orphaned subtask (parent gone) would otherwise vanish, so show it as a root.
                list.child(searchFiltered(new TaskRowWidget(task, data, false), data, task));
            }
        }
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

    /** Shows the row only while it matches the current search query, re-checked every tick. */
    private static TaskRowWidget searchFiltered(TaskRowWidget row, TaskNHGuiData data, Task task) {
        row.setEnabledIf(w -> matchesQuery(task, query(data)));
        return row;
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
