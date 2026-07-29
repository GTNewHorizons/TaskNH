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
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final int dimensionId;
    private final String title;
    private final TaskStatus status;
    private boolean isActiveAsWaypoint;

    public TaskMapLocation(Task task) {
        Objects.requireNonNull(task.location, "task must have a location to be shown on map");
        this.taskId = task.id;
        this.blockX = task.location.x;
        this.blockY = task.location.y;
        this.blockZ = task.location.z;
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
        return blockX + 0.5;
    }

    @Override
    public double getBlockZ() {
        return blockZ + 0.5;
    }

    @Override
    public int getDimensionId() {
        return dimensionId;
    }

    @Override
    public long toLong() {
        // Navigator requires one stable 64-bit identity; folding the task UUID keeps colocated tasks independent.
        return taskId.getMostSignificantBits() ^ taskId.getLeastSignificantBits();
    }

    public int getColor() {
        return switch (status) {
            case OPEN -> ColorUtils.MAP_WAYPOINT_OPEN.getColor();
            case IN_PROGRESS -> ColorUtils.MAP_WAYPOINT_IN_PROGRESS.getColor();
            case DONE -> ColorUtils.MAP_WAYPOINT_DONE.getColor();
        };
    }

    @Override
    public Waypoint toWaypoint() {
        return new Waypoint(blockX, blockY, blockZ, dimensionId, title, getColor());
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
        isActiveAsWaypoint = waypoint.dimensionId == dimensionId && waypoint.blockX == blockX
            && waypoint.blockY == blockY
            && waypoint.blockZ == blockZ;
    }
}
