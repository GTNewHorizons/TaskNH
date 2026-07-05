package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.eldrinn.foreman.data.Subtask;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskLocation;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ColorUtils;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.CreateTaskPacket;
import com.eldrinn.foreman.network.DeleteTaskPacket;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.RemindTaskPacket;
import com.eldrinn.foreman.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskDetailWidget extends Flow {

    private static final int ROW_H = 24;
    private static final int EL_H = 20;
    private static final int SCROLLBAR_W = 4;

    private static final UITexture ICON_ADD = GuiTextures.ADD.withColorOverride(ColorUtils.ICON_ADD.getColor());
    private static final UITexture ICON_REMOVE = GuiTextures.REMOVE
        .withColorOverride(ColorUtils.ICON_REMOVE.getColor());

    private final ForemanGuiData data;
    private final Task task;
    private final boolean isNew;
    // Tracks whether CreateTaskPacket has been sent so subsequent edits use UpdateTaskPacket
    private boolean created = false;
    @SuppressWarnings("rawtypes")
    private final ListWidget formList;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TaskDetailWidget(ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        this.data = data;
        int height = ForemanGui.getHeight();
        int innerH = height - 2 * ForemanGui.PADDING;
        int innerW = ForemanGui.LEFT_WIDTH - 2 * ForemanGui.PADDING;
        size(ForemanGui.LEFT_WIDTH, height);
        padding(ForemanGui.PADDING);

        this.formList = new ListWidget();
        this.formList.scrollDirection(new VerticalScrollData(false, SCROLLBAR_W));
        this.formList.size(innerW, innerH);
        child(this.formList);

        if (data.createMode) {
            this.task = new Task(UUID.randomUUID(), "", "", TaskStatus.OPEN);
            this.isNew = true;
            buildForm();
        } else if (data.selectedTaskId != null) {
            Task found = ForemanClientCache.get(data.selectedTaskId);
            this.task = found;
            this.isNew = false;
            if (found != null) {
                buildForm();
            } else {
                formList.child(new TextWidget<>(t("foreman.gui.not_found")));
            }
        } else {
            this.task = null;
            this.isNew = false;
            formList.child(new TextWidget<>(t("foreman.gui.placeholder")));
        }
    }

    @SuppressWarnings("unchecked")
    private void buildForm() {
        // Reserve space for the vertical scrollbar so it doesn't overlap content
        final int W = ForemanGui.LEFT_WIDTH - 2 * ForemanGui.PADDING - SCROLLBAR_W;

        // Header: [back 20] [gap 4] [icon 20] [title fills rest] [delete 20] [pin 20]
        final int BACK_BTN_W = EL_H; // 20px
        final int HEADER_GAP = 4;
        final int titleW = isNew ? W - BACK_BTN_W - HEADER_GAP - EL_H : W - BACK_BTN_W - HEADER_GAP - EL_H * 3;
        Flow header = Flow.row()
            .size(W, ROW_H);

        // Back button
        header.child(
            new ButtonWidget<>().size(BACK_BTN_W, EL_H)
                .overlay(GuiTextures.LEFTLOAD)
                .onMousePressed(btn -> {
                    if (btn != 0) return false;
                    data.clear();
                    data.pageController.setPage(0);
                    return true;
                }));

        var headerGap = new TextWidget<>("");
        headerGap.size(HEADER_GAP, EL_H);
        header.child(headerGap);

        // Icon slot — drag from NEI to set, right-click to clear
        header.child(new IconSlotWidget(new IconSlotWidget.ItemHolder() {

            @Override
            public String get() {
                return task.iconItem;
            }

            @Override
            public void set(String v) {
                task.iconItem = v;
            }
        }, () -> {
            sendUpdate();
            ForemanGui.open(data);
        }).size(EL_H, EL_H));
        PlainTextField titleField = new PlainTextField();
        titleField.size(titleW, EL_H);
        titleField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        titleField.value(new StringValue.Dynamic(() -> task.title, val -> {
            task.title = val;
            sendUpdate();
        }));
        header.child(titleField);
        if (!isNew) {
            header.child(
                new ButtonWidget<>().size(EL_H, EL_H)
                    .overlay(ICON_REMOVE)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        ForemanNetwork.CHANNEL.sendToServer(new DeleteTaskPacket(task.id));
                        data.clear();
                        ForemanGui.open(data);
                        return true;
                    }));

            boolean pinned = ForemanClientCache.isPinned(task.id);
            boolean canPin = ForemanClientCache.canPin();
            UITexture pinIcon;
            if (pinned) {
                pinIcon = GuiTextures.FAVORITE.withColorOverride(ColorUtils.PIN_ACTIVE.getColor());
            } else if (canPin) {
                pinIcon = GuiTextures.FAVORITE_OUTLINE;
            } else {
                pinIcon = GuiTextures.FAVORITE_OUTLINE.withColorOverride(ColorUtils.PIN_INACTIVE.getColor());
            }
            ButtonWidget<?> pinBtn = new ButtonWidget<>();
            pinBtn.size(EL_H, EL_H);
            pinBtn.overlay(pinIcon);
            header.child(pinBtn.onMousePressed(btn -> {
                if (btn != 0) return false;
                if (pinned) {
                    ForemanClientCache.unpin(task.id);
                } else {
                    ForemanClientCache.pin(task.id);
                }
                ForemanGui.open(data);
                return true;
            }));
        }
        formList.child(header);

        // Description
        var descLabel = new TextWidget<>(t("foreman.gui.detail.description"));
        descLabel.size(W, 14);
        formList.child(descLabel);
        PlainTextField descField = new PlainTextField();
        descField.size(W, EL_H);
        descField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        descField.value(new StringValue.Dynamic(() -> task.description, val -> {
            task.description = val;
            sendUpdate();
        }));
        formList.child(descField);

        // Status buttons
        var statusLabel = new TextWidget<>(t("foreman.gui.detail.status"));
        statusLabel.size(W, 14);
        formList.child(statusLabel);
        Flow statusRow = Flow.row()
            .size(W, ROW_H);
        TaskStatus[] statuses = TaskStatus.values();
        final int STATUS_BTN_W = W / statuses.length;
        final int STATUS_LAST_W = W - STATUS_BTN_W * (statuses.length - 1);
        for (int i = 0; i < statuses.length; i++) {
            TaskStatus status = statuses[i];
            int btnW = (i == statuses.length - 1) ? STATUS_LAST_W : STATUS_BTN_W;
            var normalLabel = new TextWidget<>(status.displayName());
            normalLabel.size(btnW, EL_H);
            normalLabel.textAlign(Alignment.Center);
            normalLabel.color(ColorUtils.TEXT_WHITE.getColor());
            var activeLabel = new TextWidget<>(status.displayName());
            activeLabel.size(btnW, EL_H);
            activeLabel.textAlign(Alignment.Center);
            activeLabel.color(ColorUtils.TEXT_WHITE.getColor());
            statusRow.child(
                new ToggleButton().size(btnW, EL_H)
                    .value(new BoolValue.Dynamic(() -> task.status == status, selected -> {
                        if (selected) {
                            task.status = status;
                            sendUpdate();
                            ForemanGui.open(data);
                        }
                    }))
                    .child(false, normalLabel)
                    .child(true, activeLabel));
        }
        formList.child(statusRow);

        // Assignees
        var assigneesLabel = new TextWidget<>(t("foreman.gui.detail.assignees"));
        assigneesLabel.size(W, 14);
        formList.child(assigneesLabel);
        formList.child(new AssigneePickerWidget(task, data, W));

        // Remind buttons for each assigned player
        if (!task.assignees.isEmpty()) {
            final int HEAD_SIZE = 8;
            final int GAP = 4;
            int remindW = Minecraft.getMinecraft().fontRenderer.getStringWidth("Remind") + 12;
            int nameW = W - GAP - HEAD_SIZE - GAP - remindW - GAP;

            for (com.eldrinn.foreman.data.AssignedPlayer ap : task.assignees) {
                String apName = ForemanClientCache.getTeamMembers()
                    .stream()
                    .filter(
                        e -> e.id()
                            .equals(ap.playerId()))
                    .map(PlayerEntry::name)
                    .findFirst()
                    .orElseGet(
                        () -> ap.playerId()
                            .toString()
                            .substring(0, 8));

                Flow assigneeRow = Flow.row()
                    .size(W, ROW_H);
                assigneeRow.child(
                    new PlayerHeadWidget(apName).size(HEAD_SIZE, HEAD_SIZE)
                        .marginTop(6)
                        .marginLeft(GAP));
                assigneeRow.child(
                    new TextWidget<>(apName).size(nameW, ROW_H)
                        .textAlign(Alignment.CenterLeft)
                        .marginLeft(GAP));
                assigneeRow.child(
                    new ButtonWidget<>().size(remindW, EL_H)
                        .marginTop(2)
                        .marginLeft(GAP)
                        .child(
                            new TextWidget<>("Remind").size(remindW, EL_H)
                                .textAlign(Alignment.Center))
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            ForemanNetwork.CHANNEL.sendToServer(new RemindTaskPacket(task.id, ap.playerId()));
                            return true;
                        }));
                formList.child(assigneeRow);
            }
        }

        // Location header: [label fills rest] [show-on-map label 72] [4px gap] [toggle EL_H]
        final int MAP_LABEL_W = 72;
        final int MAP_GAP = 4;
        Flow locationHeader = Flow.row()
            .size(W, ROW_H);
        var locationLabel = new TextWidget<>(t("foreman.gui.detail.location"));
        locationLabel.size(W - MAP_LABEL_W - MAP_GAP - EL_H, EL_H);
        locationLabel.textAlign(Alignment.CenterLeft);
        locationHeader.child(locationLabel);
        var showMapLabel = new TextWidget<>(t("foreman.gui.detail.show_on_map"));
        showMapLabel.size(MAP_LABEL_W, EL_H);
        showMapLabel.textAlign(Alignment.CenterRight);
        locationHeader.child(showMapLabel);
        var mapSpacer = new TextWidget<>("");
        mapSpacer.size(MAP_GAP, EL_H);
        locationHeader.child(mapSpacer);
        locationHeader.child(
            new ToggleButton().size(EL_H, EL_H)
                .value(new BoolValue.Dynamic(() -> task.showOnMap, val -> {
                    task.showOnMap = val;
                    sendUpdate();
                }))
                .overlay(true, GuiTextures.CHECKMARK));
        formList.child(locationHeader);
        formList.child(buildLocationRow());

        // Subtasks
        var subtasksLabel = new TextWidget<>(t("foreman.gui.detail.subtasks"));
        subtasksLabel.size(W, 14);
        formList.child(subtasksLabel);
        formList.child(buildSubtaskList());
    }

    private Flow buildLocationRow() {
        final int W = ForemanGui.LEFT_WIDTH - 2 * ForemanGui.PADDING - SCROLLBAR_W;
        // Layout: [x: LABEL][field][GAP][y: LABEL][field][GAP][z: LABEL][field][Pos POS]
        final int LABEL_W = 12;
        final int GAP_W = 4;
        final int POS_W = 44;
        final int FIELD_W = (W - 3 * LABEL_W - 2 * GAP_W - POS_W) / 3;
        Flow row = Flow.row()
            .size(W, ROW_H);

        var xLabel = new TextWidget<>("x:");
        xLabel.size(LABEL_W, EL_H);
        xLabel.textAlign(Alignment.CenterLeft);
        row.child(xLabel);
        PlainTextField xField = new PlainTextField();
        xField.size(FIELD_W, EL_H);
        xField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        xField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.x : 0), val -> {
            TaskLocation loc = ensureLocation();
            try {
                loc.x = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(xField);

        // gap before y:
        var gap1 = new TextWidget<>("");
        gap1.size(GAP_W, EL_H);
        row.child(gap1);
        var yLabel = new TextWidget<>("y:");
        yLabel.size(LABEL_W, EL_H);
        yLabel.textAlign(Alignment.CenterLeft);
        row.child(yLabel);
        PlainTextField yField = new PlainTextField();
        yField.size(FIELD_W, EL_H);
        yField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        yField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.y : 0), val -> {
            TaskLocation loc = ensureLocation();
            try {
                loc.y = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(yField);

        // gap before z:
        var gap2 = new TextWidget<>("");
        gap2.size(GAP_W, EL_H);
        row.child(gap2);
        var zLabel = new TextWidget<>("z:");
        zLabel.size(LABEL_W, EL_H);
        zLabel.textAlign(Alignment.CenterLeft);
        row.child(zLabel);
        PlainTextField zField = new PlainTextField();
        zField.size(FIELD_W, EL_H);
        zField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        zField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.z : 0), val -> {
            TaskLocation loc = ensureLocation();
            try {
                loc.z = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(zField);

        // Pos button fills remaining space
        final int actualPosW = W - 3 * LABEL_W - 3 * FIELD_W - 2 * GAP_W;
        var posLabel = new TextWidget<>(t("foreman.gui.detail.pos"));
        posLabel.size(actualPosW, EL_H);
        posLabel.textAlign(Alignment.Center);
        posLabel.color(ColorUtils.TEXT_WHITE.getColor());
        row.child(
            new ButtonWidget<>().size(actualPosW, EL_H)
                .child(posLabel)
                .onMousePressed(btn -> {
                    if (btn != 0) return false;
                    EntityPlayer p = Minecraft.getMinecraft().thePlayer;
                    TaskLocation loc = ensureLocation();
                    loc.x = (int) p.posX;
                    loc.y = (int) p.posY;
                    loc.z = (int) p.posZ;
                    loc.dimension = p.worldObj.provider.dimensionId;
                    sendUpdate();
                    ForemanGui.open(data);
                    return true;
                }));

        return row;
    }

    private Flow buildSubtaskList() {
        final int W = ForemanGui.LEFT_WIDTH - 2 * ForemanGui.PADDING - SCROLLBAR_W;
        Flow col = Flow.column();
        col.size(W, ROW_H);
        col.coverChildrenHeight(ROW_H);

        for (Subtask sub : task.subtasks) {
            var subtaskTitle = new TextWidget<>(sub.title);
            subtaskTitle.size(W - EL_H * 2 - 4, EL_H);
            subtaskTitle.textAlign(Alignment.CenterLeft);
            subtaskTitle.marginLeft(4);
            ButtonWidget<?> checkBtn = new ButtonWidget<>();
            checkBtn.size(EL_H, EL_H);
            if (sub.checked) checkBtn.overlay(GuiTextures.CHECKMARK);
            col.child(
                Flow.row()
                    .size(W, ROW_H)
                    .child(checkBtn.onMousePressed(btn -> {
                        if (btn != 0) return false;
                        sub.checked = !sub.checked;
                        sendUpdate();
                        ForemanGui.open(data);
                        return true;
                    }))
                    .child(subtaskTitle)
                    .child(
                        new ButtonWidget<>().size(EL_H, EL_H)
                            .overlay(ICON_REMOVE)
                            .onMousePressed(btn -> {
                                if (btn != 0) return false;
                                task.subtasks.remove(sub);
                                sendUpdate();
                                ForemanGui.open(data);
                                return true;
                            })));
        }

        // Add subtask row
        String[] newTitle = { "" };
        PlainTextField addField = new PlainTextField();
        addField.size(W - EL_H, EL_H);
        addField.setTextColor(ColorUtils.TEXT_WHITE.getColor());
        addField.autoUpdateOnChange(true);
        addField.value(new StringValue.Dynamic(() -> newTitle[0], val -> newTitle[0] = val));
        // Re-focus the add field after a rebuild triggered by Enter, so the user can keep typing.
        if (data.focusSubtaskAdd) {
            addField.setFocusOnGuiOpen(true);
            data.focusSubtaskAdd = false;
        }
        // refocus=true: skip our own rebuild and let the server sync rebuild once, then refocus the
        // field, so a second rebuild doesn't steal the cursor (Enter keeps you typing subtasks).
        java.util.function.Consumer<Boolean> addSubtask = refocus -> {
            String title = newTitle[0].trim();
            if (title.isEmpty()) return;
            task.subtasks.add(new Subtask(UUID.randomUUID(), title, false));
            newTitle[0] = "";
            if (refocus) {
                data.focusSubtaskAdd = true;
                sendUpdate();
                // No packet (and thus no sync rebuild) goes out for an unsaved new task, so rebuild locally.
                if (isNew && !created) ForemanGui.open(data);
            } else {
                sendUpdate();
                ForemanGui.open(data);
            }
        };
        addField.onEnter(() -> addSubtask.accept(true));
        col.child(
            Flow.row()
                .size(W, ROW_H)
                .child(addField)
                .child(
                    new ButtonWidget<>().size(EL_H, EL_H)
                        .overlay(ICON_ADD)
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            addSubtask.accept(false);
                            return true;
                        })));
        return col;
    }

    private TaskLocation ensureLocation() {
        if (task.location == null) {
            task.location = new TaskLocation(0, 0, 0, 0, "");
        }
        return task.location;
    }

    private static String t(String key) {
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    private void sendUpdate() {
        if (isNew) {
            if (!task.title.isEmpty()) {
                if (!created) {
                    ForemanNetwork.CHANNEL.sendToServer(new CreateTaskPacket(task));
                    data.selectTask(task.id);
                    created = true;
                } else {
                    ForemanNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
                }
            }
        } else {
            ForemanNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
        }
    }
}
