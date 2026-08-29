package com.eldrinn.tasknh.integration;

import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.Loader;

/**
 * Availability check for the BetterQuesting integration.
 * Must not reference any betterquesting class directly, so that it stays loadable without BetterQuesting.
 */
public final class BQIntegration {

    /** Context menu API, added in BetterQuesting 3.8.72-GTNH. */
    private static final String CONTEXT_MENU_API = "betterquesting.api2.client.gui.context.IQuestContextMenuEntry";

    private static Boolean available;

    private BQIntegration() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = check();
            if (!available) {
                LogManager.getLogger("tasknh")
                    .info("BetterQuesting integration disabled: {} is not available", CONTEXT_MENU_API);
            }
        }
        return available;
    }

    private static boolean check() {
        if (!Loader.isModLoaded("betterquesting")) return false;
        try {
            Class.forName(CONTEXT_MENU_API, false, BQIntegration.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
