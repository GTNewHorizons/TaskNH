package com.eldrinn.tasknh.navigator;

import java.util.Objects;
import java.util.UUID;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskMapLocation implements IWaypointAndLocationProvider {

    private final UUID taskId;
    private final double blockX;
    private final double blockZ;
    private final int dimensionId;
    private final String title;
    private final TaskStatus status;
    private boolean isActiveAsWaypoint;

    public TaskMapLocation(Task task) {
        Objects.requireNonNull(task.location, "task must have a location to be shown on map");
        this.taskId = task.id;
        this.blockX = task.location.x + 0.5;
        this.blockZ = task.location.z + 0.5;
        this.dimensionId = task.location.dimension;
        this.title = task.title;
        this.status = task.status;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public double getBlockX() {
        return blockX;
    }

    @Override
    public double getBlockZ() {
        return blockZ;
    }

    @Override
    public int getDimensionId() {
        return dimensionId;
    }

    @Override
    public Waypoint toWaypoint() {
        int color = switch (status) {
            case OPEN -> ColorUtils.MAP_WAYPOINT_OPEN.getColor();
            case IN_PROGRESS -> ColorUtils.MAP_WAYPOINT_IN_PROGRESS.getColor();
            case DONE -> ColorUtils.MAP_WAYPOINT_DONE.getColor();
        };
        return new Waypoint((int) blockX, 64, (int) blockZ, dimensionId, title, color);
    }

    @Override
    public boolean isActiveAsWaypoint() {
        return isActiveAsWaypoint;
    }

    @Override
    public void onWaypointCleared() {
        isActiveAsWaypoint = false;
    }

    @Override
    public void onWaypointUpdated(Waypoint waypoint) {
        isActiveAsWaypoint = waypoint.dimensionId == dimensionId && waypoint.blockX == (int) blockX
            && waypoint.blockZ == (int) blockZ;
    }
}
