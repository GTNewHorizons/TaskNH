package com.eldrinn.tasknh.config;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Server-side config for item tracking. */
public final class TaskNHConfig {

    private static final String CATEGORY = "item_tracking";

    /** Whether tasks with a tracked item auto-complete when that item is seen in a member's inventory. */
    public static boolean itemTrackingEnabled = true;

    /** Whether to announce an auto-completed task in team chat. */
    public static boolean announceAutoComplete = true;

    private TaskNHConfig() {}

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        itemTrackingEnabled = config.getBoolean(
            "enabled",
            CATEGORY,
            true,
            "Auto-complete a task when its tracked item appears in a team member's inventory.");
        announceAutoComplete = config
            .getBoolean("announce", CATEGORY, true, "Send a chat message to the team when a task auto-completes.");
        if (config.hasChanged()) config.save();
    }
}
