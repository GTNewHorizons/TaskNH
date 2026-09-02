package com.eldrinn.tasknh.gui;

import java.util.UUID;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.widgets.PagedWidget;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.widget.ScrollMemoryList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskNHGuiData {

    /** Status tab currently shown in the list. */
    @Nullable
    public TaskStatus activeTab = TaskStatus.OPEN;

    /** UUID of the task selected in the list, or null if none. */
    @Nullable
    public UUID selectedTaskId = null;

    /** True when "New Task" was clicked — right panel shows empty create form. */
    public boolean createMode = false;

    /** Current search query; empty string means no filter. */
    public String searchQuery = "";

    /** Whether the search field is expanded. */
    public boolean searchExpanded = false;

    /** Scroll state of the task list, kept across rebuilds. */
    public final ScrollMemoryList.Memory listScroll = new ScrollMemoryList.Memory();

    /** Scroll state of the task detail form, kept across rebuilds. */
    public final ScrollMemoryList.Memory detailScroll = new ScrollMemoryList.Memory();

    public final PagedWidget.Controller pageController = new PagedWidget.Controller();

    public void selectTask(UUID id) {
        this.selectedTaskId = id;
        this.createMode = false;
        this.detailScroll.reset();
    }

    public void enterCreateMode() {
        this.selectedTaskId = null;
        this.createMode = true;
        this.detailScroll.reset();
    }

    public void clear() {
        this.selectedTaskId = null;
        this.createMode = false;
        this.detailScroll.reset();
    }
}
