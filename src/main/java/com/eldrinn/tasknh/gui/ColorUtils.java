package com.eldrinn.tasknh.gui;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("tasknh");

    public static final ColorResource
    // spotless:off
        textWhite               = color.rgb("textWhite",                "0xFFFFFF"),
        textGray                = color.rgb("textGray",                 "0xAAAAAA"),

        accentGold              = color.rgb("accentGold",               "0xF0C040"),
        accentGreen             = color.rgb("accentGreen",              "0x8BC34A"),

        mapWaypointOpen         = color.rgb("mapWaypointOpen",          "0xFFAAAA"),
        mapWaypointInProgress   = color.rgb("mapWaypointInProgress",    "0xAAAAFF"),
        mapWaypointDone         = color.rgb("mapWaypointDone",          "0xAAFFAA"),

        mapFillOpen             = color.rgb("mapFillOpen",              "0x888800"),
        mapFillInProgress       = color.rgb("mapFillInProgress",        "0x004488"),
        mapFillDone             = color.rgb("mapFillDone",              "0x228822"),

        mapText                 = color.rgb("mapText",                  "0xFFFFFF"),
        mapBorder               = color.rgb("mapBorder",                "0xFFFFFF"),

        iconAdd                 = color.argb("iconAdd",                 "0xFF40C040"),
        iconRemove              = color.argb("iconRemove",              "0xFFC04040"),
        iconPinActive           = color.argb("iconPinActive",           "0xFFF0C040"),
        iconPinInactive         = color.argb("iconPinInactive",         "0xFF555555"),

        backgroundHud           = color.argb("backgroundHud",           "0x88000000"),
        backgroundOverlay       = color.argb("backgroundOverlay",       "0x44000000"),
        backgroundPanel         = color.argb("backgroundPanel",         "0xCC000000"),
        backgroundButton        = color.argb("backgroundButton",        "0xFF444444"),
        backgroundDanger        = color.argb("backgroundDanger",        "0xFF884444"),
        backgroundHandle        = color.argb("backgroundHandle",        "0xFFCC3333");
    // spotless:on
}
