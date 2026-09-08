package com.eldrinn.tasknh.config;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Server-side config for item tracking and reminders. */
public final class TaskNHConfig {

    private static final String CATEGORY = "item_tracking";
    private static final String CATEGORY_REMINDERS = "reminders";

    /** Whether tasks with a tracked item auto-complete when that item is seen in a member's inventory. */
    public static boolean itemTrackingEnabled = true;

    /** Whether to announce an auto-completed task in team chat. */
    public static boolean announceAutoComplete = true;

    /** How long a player must wait before reminding the same player of the same task again. */
    public static int remindCooldownSeconds = 60;

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
        remindCooldownSeconds = config.getInt(
            "cooldown",
            CATEGORY_REMINDERS,
            60,
            0,
            3600,
            "Seconds a player must wait before reminding the same player of the same task again. 0 disables the cooldown.");
        if (config.hasChanged()) config.save();
    }
}
