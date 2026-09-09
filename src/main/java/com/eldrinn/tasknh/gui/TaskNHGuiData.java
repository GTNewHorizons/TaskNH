package com.eldrinn.tasknh.gui;

import java.util.HashSet;
import java.util.Set;
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

    /** Whether the tracked item count field is shown under the slot. Toggled by middle-clicking the slot. */
    public boolean trackCountExpanded = false;

    /**
     * Parent tasks whose done subtasks are shown in the list. Static so the choice survives closing
     * the GUI, which builds a new instance every time. Cleared of unknown ids on every sync,
     * together with stale pins, see TaskNHClientCache#update.
     */
    public static final Set<UUID> shownDoneChildren = new HashSet<>();

    /** Scroll state of the task list, kept across rebuilds. */
    public final ScrollMemoryList.Memory listScroll = new ScrollMemoryList.Memory();

    /** Scroll state of the task detail form, kept across rebuilds. */
    public final ScrollMemoryList.Memory detailScroll = new ScrollMemoryList.Memory();

    public final PagedWidget.Controller pageController = new PagedWidget.Controller();

    public void selectTask(UUID id) {
        this.selectedTaskId = id;
        this.createMode = false;
        this.detailScroll.reset();
        this.trackCountExpanded = false;
    }

    public void enterCreateMode() {
        this.selectedTaskId = null;
        this.createMode = true;
        this.detailScroll.reset();
        this.trackCountExpanded = false;
    }

    public void clear() {
        this.selectedTaskId = null;
        this.createMode = false;
        this.detailScroll.reset();
        this.trackCountExpanded = false;
    }
}
