package com.eldrinn.tasknh.navigator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;
import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalInteractableRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.WaypointManager;
import com.gtnewhorizons.navigator.api.util.ClickPos;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskLayerManager extends InteractableLayerManager {

    public static final TaskLayerManager INSTANCE = new TaskLayerManager();

    private final List<TaskMapLocation> locations = new ArrayList<>();

    private TaskLayerManager() {
        super(TaskMapButtonManager.INSTANCE);
    }

    @Override
    protected @Nullable LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return new UniversalInteractableRenderer(manager).withClickAction(this::onClick)
            .withRenderStep(loc -> new TaskMapRenderStep((TaskMapLocation) loc));
    }

    @Override
    protected @Nullable WaypointManager addWaypointManager(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMWaypointManager(manager);
            case XaeroWorldMap -> new XaeroWaypointManager(manager);
            default -> null;
        };
    }

    /**
     * Called by LayerManager when recaching. Returns a task marker if one exists at this chunk/dim.
     */
    @Override
    protected @Nullable ILocationProvider generateLocation(int chunkX, int chunkZ, int dim) {
        for (TaskMapLocation loc : locations) {
            if (loc.getDimensionId() == dim && loc.getChunkX() == chunkX && loc.getChunkZ() == chunkZ) {
                return loc;
            }
        }
        return null;
    }

    /**
     * Replaces all map markers with the current task list.
     * Only tasks with a non-null location are shown.
     */
    public void refreshFromCache(Collection<Task> tasks) {
        locations.clear();
        for (Task task : tasks) {
            if (task.location != null && task.showOnMap) {
                locations.add(new TaskMapLocation(task));
            }
        }
        clearFullCache();
        forceRefresh();
    }

    private boolean onClick(ClickPos pos) {
        if (pos.getRenderStep() == null) return false;
        TaskMapLocation loc = (TaskMapLocation) pos.getRenderStep()
            .getLocation();
        TaskNHGuiData data = new TaskNHGuiData();
        data.selectTask(loc.getTaskId());
        TaskNHGui.open(data);
        return true;
    }
}
