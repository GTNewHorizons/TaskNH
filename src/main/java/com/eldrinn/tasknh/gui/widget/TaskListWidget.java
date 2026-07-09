package com.eldrinn.tasknh.gui.widget;

import java.util.Collection;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
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
            searchField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
            searchField.autoUpdateOnChange(true);
            searchField.value(new StringValue.Dynamic(() -> data.searchQuery, val -> {
                data.searchQuery = val;
                TaskNHGui.open(data);
            }));
            searchRow.child(searchField);
        }
        child(searchRow);

        // Task list filtered by active tab and search query
        ListWidget list = new ListWidget();
        list.scrollDirection(new VerticalScrollData(false, TaskRowWidget.SCROLLBAR_W));
        list.size(W, H - 24 - P - 20 - P - 28);
        list.marginTop(P);
        String query = data.searchQuery.toLowerCase();
        for (Task task : allTasks) {
            if (task.status != data.activeTab) continue;
            if (!query.isEmpty() && !task.title.toLowerCase()
                .contains(query)
                && !task.description.toLowerCase()
                    .contains(query))
                continue;
            list.child(new TaskRowWidget(task, data));
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

    private static String tabLabel(String key, TaskStatus status, Collection<Task> tasks) {
        long count = tasks.stream()
            .filter(t -> t.status == status)
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
