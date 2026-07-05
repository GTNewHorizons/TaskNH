package com.eldrinn.foreman.gui;

import java.util.UUID;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.widgets.PagedWidget;
import com.eldrinn.foreman.data.TaskStatus;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ForemanGuiData {

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

    /** One-shot: focus the subtask add field after the next rebuild (Enter keeps typing). */
    public boolean focusSubtaskAdd = false;

    public final PagedWidget.Controller pageController = new PagedWidget.Controller();

    public void selectTask(UUID id) {
        this.selectedTaskId = id;
        this.createMode = false;
    }

    public void enterCreateMode() {
        this.selectedTaskId = null;
        this.createMode = true;
    }

    public void clear() {
        this.selectedTaskId = null;
        this.createMode = false;
    }
}
