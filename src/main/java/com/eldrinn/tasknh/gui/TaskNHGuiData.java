package com.eldrinn.tasknh.gui;

import java.util.UUID;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.widgets.PagedWidget;
import com.eldrinn.tasknh.data.Task;
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

    /** The task being filled in create mode. Kept here so a rebuild, such as setting the icon, keeps the input. */
    @Nullable
    public Task draft = null;

    /** Current search query; empty string means no filter. */
    public String searchQuery = "";

    /** Whether the search field is expanded. */
    public boolean searchExpanded = false;

    /** Whether the tracked item count field is shown under the slot. Toggled by middle-clicking the slot. */
    public boolean trackCountExpanded = false;

    /** Checklist item whose count field is shown under its row, or null. Toggled by middle-clicking its slot. */
    @Nullable
    public UUID checklistCountExpanded = null;

    /** Scroll state of the task list, kept across rebuilds. */
    public final ScrollMemoryList.Memory listScroll = new ScrollMemoryList.Memory();

    /** Scroll state of the task detail form, kept across rebuilds. */
    public final ScrollMemoryList.Memory detailScroll = new ScrollMemoryList.Memory();

    /** Detail scroll of the task a subtask was opened from, restored by the link back to it. */
    public final ScrollMemoryList.Memory parentScroll = new ScrollMemoryList.Memory();

    /** Task that {@link #parentScroll} belongs to, or null. */
    @Nullable
    public UUID parentScrollOwner = null;

    public final PagedWidget.Controller pageController = new PagedWidget.Controller();

    public void selectTask(UUID id) {
        this.selectedTaskId = id;
        this.createMode = false;
        this.draft = null;
        this.detailScroll.reset();
        this.trackCountExpanded = false;
        this.checklistCountExpanded = null;
    }

    public void enterCreateMode() {
        this.selectedTaskId = null;
        this.createMode = true;
        this.draft = new Task(UUID.randomUUID(), "", "", TaskStatus.OPEN);
        this.detailScroll.reset();
        this.trackCountExpanded = false;
        this.checklistCountExpanded = null;
    }

    public void clear() {
        this.selectedTaskId = null;
        this.createMode = false;
        this.draft = null;
        this.detailScroll.reset();
        this.trackCountExpanded = false;
        this.checklistCountExpanded = null;
        // Back to the list: a subtask opened from there next must not reuse the parent's old scroll.
        this.parentScrollOwner = null;
    }
}
