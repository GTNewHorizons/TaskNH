package com.eldrinn.tasknh.navigator;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.gui.TaskNHGuiData;
import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalInteractableRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.markers.MapMarker;
import com.gtnewhorizons.navigator.api.model.waypoints.WaypointManager;
import com.gtnewhorizons.navigator.api.util.ClickPos;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskLayerManager extends InteractableLayerManager {

    public static final TaskLayerManager INSTANCE = new TaskLayerManager();

    private static final Map<TaskStatus, BufferedImage> MARKER_IMAGES = new EnumMap<>(TaskStatus.class);

    private final List<TaskMapLocation> locations = new ArrayList<>();

    private TaskLayerManager() {
        super(TaskMapButtonManager.INSTANCE);
    }

    @Override
    protected @Nullable LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return new UniversalInteractableRenderer(manager).withClickAction(this::onClick)
            .withRenderStep(loc -> new TaskMapRenderStep((TaskMapLocation) loc))
            .withMapMarker(loc -> createMapMarker((TaskMapLocation) loc));
    }

    @Override
    protected @Nullable WaypointManager addWaypointManager(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMWaypointManager(manager);
            case XaeroWorldMap -> new XaeroWaypointManager(manager);
            default -> null;
        };
    }

    @Override
    protected Collection<? extends ILocationProvider> generateVisibleLocations(int minBlockX, int minBlockZ,
        int maxBlockX, int maxBlockZ, int dimension) {
        List<TaskMapLocation> visible = new ArrayList<>();
        for (TaskMapLocation loc : locations) {
            if (loc.getDimensionId() == dimension && loc.getBlockX() >= minBlockX
                && loc.getBlockX() <= maxBlockX
                && loc.getBlockZ() >= minBlockZ
                && loc.getBlockZ() <= maxBlockZ) visible.add(loc);
        }
        return visible;
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
    }

    private boolean onClick(ClickPos pos) {
        if (pos.isDoubleClick() || pos.getLocationRenderStep() == null) return false;
        TaskMapLocation loc = (TaskMapLocation) pos.getLocationRenderStep()
            .getLocation();
        TaskNHGuiData data = new TaskNHGuiData();
        data.selectTask(loc.getTaskId());
        TaskNHGui.open(data);
        return true;
    }

    private static MapMarker createMapMarker(TaskMapLocation location) {
        return new MapMarker(MARKER_IMAGES.computeIfAbsent(location.getStatus(), TaskLayerManager::createMarkerImage))
            .setDisplaySize(16, 16)
            .setDisplayZoomScale(1, 2, 3, 5)
            .setLabel(location.getTitle())
            .setLabelColor(location.getColor())
            .setLabelScale(1.2F)
            .setLabelZoomScale(1, 2.5, 2, 5)
            .setLabelOffsetY(18)
            .setLabelBackgroundOpacity(0.35F)
            .setLabelMinZoom(2)
            .setLabelOnMinimap(false);
    }

    private static BufferedImage createMarkerImage(TaskStatus status) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int fill = 0xC8000000 | (switch (status) {
            case OPEN -> ColorUtils.MAP_FILL_OPEN.getColor();
            case IN_PROGRESS -> ColorUtils.MAP_FILL_IN_PROGRESS.getColor();
            case DONE -> ColorUtils.MAP_FILL_DONE.getColor();
        } & 0xFFFFFF);
        int border = 0xB4000000 | (ColorUtils.MAP_BORDER.getColor() & 0xFFFFFF);
        int text = ColorUtils.MAP_TEXT.getColor();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, x == 0 || x == 15 || y == 0 || y == 15 ? border : fill);
            }
        }

        String[] glyph = switch (status) {
            case OPEN -> new String[] { "111", "101", "101", "101", "111" };
            case IN_PROGRESS -> new String[] { "000", "000", "110", "011", "000" };
            case DONE -> new String[] { "101", "101", "101", "101", "010" };
        };
        for (int y = 0; y < glyph.length; y++) {
            for (int x = 0; x < glyph[y].length(); x++) {
                if (glyph[y].charAt(x) == '1') {
                    image.setRGB(5 + x * 2, 3 + y * 2, text);
                    image.setRGB(6 + x * 2, 3 + y * 2, text);
                    image.setRGB(5 + x * 2, 4 + y * 2, text);
                    image.setRGB(6 + x * 2, 4 + y * 2, text);
                }
            }
        }
        return image;
    }
}
