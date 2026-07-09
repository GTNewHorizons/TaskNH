package com.eldrinn.tasknh.gui;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public final class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("tasknh");

    // Text
    public static final ColorResource TEXT_WHITE = color.rgb("textWhite", "0xFFFFFF");
    public static final ColorResource TEXT_GRAY = color.rgb("textGray", "0xAAAAAA");

    // Accents
    public static final ColorResource GOLD = color.rgb("gold", "0xF0C040");
    public static final ColorResource GREEN = color.rgb("green", "0x8BC34A");

    // Icons
    public static final ColorResource ICON_ADD = color.argb("iconAdd", "0xFF40C040");
    public static final ColorResource ICON_REMOVE = color.argb("iconRemove", "0xFFC04040");
    public static final ColorResource PIN_ACTIVE = color.argb("pinActive", "0xFFF0C040");
    public static final ColorResource PIN_INACTIVE = color.argb("pinInactive", "0xFF555555");

    // Backgrounds
    public static final ColorResource BG_HUD = color.argb("bgHud", "0x88000000");
    public static final ColorResource BG_OVERLAY = color.argb("bgOverlay", "0x44000000");
    public static final ColorResource BG_PANEL = color.argb("bgPanel", "0xCC000000");
    public static final ColorResource BG_BUTTON = color.argb("bgButton", "0xFF444444");
    public static final ColorResource BG_DANGER = color.argb("bgDanger", "0xFF884444");
    public static final ColorResource HANDLE = color.argb("handle", "0xFFCC3333");

    // Map waypoints (light tones, Navigator API)
    public static final ColorResource MAP_WAYPOINT_OPEN = color.rgb("mapWaypointOpen", "0xFFAAAA");
    public static final ColorResource MAP_WAYPOINT_IN_PROGRESS = color.rgb("mapWaypointInProgress", "0xAAAAFF");
    public static final ColorResource MAP_WAYPOINT_DONE = color.rgb("mapWaypointDone", "0xAAFFAA");

    // Map markers fill (darker tones, separate alpha in DrawUtils)
    public static final ColorResource MAP_FILL_OPEN = color.rgb("mapFillOpen", "0x888800");
    public static final ColorResource MAP_FILL_IN_PROGRESS = color.rgb("mapFillInProgress", "0x004488");
    public static final ColorResource MAP_FILL_DONE = color.rgb("mapFillDone", "0x228822");

    // Map label
    public static final ColorResource MAP_TEXT = color.argb("mapText", "0xFFFFFFFF");
    public static final ColorResource MAP_BORDER = color.rgb("mapBorder", "0xFFFFFF");

    private ColorUtils() {}
}
