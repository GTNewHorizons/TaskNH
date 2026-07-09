package com.eldrinn.tasknh.data;

public enum TaskStatus {

    OPEN,
    IN_PROGRESS,
    DONE;

    public String displayName() {
        return switch (this) {
            case OPEN -> net.minecraft.util.StatCollector.translateToLocal("tasknh.status.open");
            case IN_PROGRESS -> net.minecraft.util.StatCollector.translateToLocal("tasknh.status.in_progress");
            case DONE -> net.minecraft.util.StatCollector.translateToLocal("tasknh.status.done");
        };
    }

    public static TaskStatus fromNBT(String name) {
        try {
            return TaskStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Unknown status in save data — fall back to OPEN and log a warning.
            // This can happen after a mod update that renames enum values.
            org.apache.logging.log4j.LogManager.getLogger("tasknh")
                .warn("Unknown TaskStatus '{}' in save data, defaulting to OPEN", name);
            return OPEN;
        }
    }
}
