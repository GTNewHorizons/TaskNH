package com.eldrinn.tasknh.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.network.OpenGuiPacket;
import com.eldrinn.tasknh.network.SyncAllTasksPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class TaskNHCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "tasknh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tasknh <list|reload|gui|create <title>|assign <id> <player>|unassign <id> <player>|done <id>|export [name]|import <name>|open <taskId>>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // subcommand-level checks handle OP restriction for reload
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            java.util.List<String> available = new java.util.ArrayList<>();
            if (TaskNHPermissions.has(sender, TaskNHPermissions.LIST)) available.add("list");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.RELOAD)) available.add("reload");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.GUI)) available.add("gui");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.CREATE)) available.add("create");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.ASSIGN)) available.add("assign");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.UNASSIGN)) available.add("unassign");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.DONE)) available.add("done");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.EXPORT)) available.add("export");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.IMPORT)) available.add("import");
            if (TaskNHPermissions.has(sender, TaskNHPermissions.OPEN)) available.add("open");
            return CommandBase.getListOfStringsMatchingLastWord(args, available.toArray(new String[0]));
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        TaskNHWorldData data = TaskNHWorldData.get();

        switch (args[0]) {
            case "list": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.LIST)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                Collection<Task> tasks = getSenderTasks(sender, data);
                if (tasks == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
                    return;
                }
                if (tasks.isEmpty()) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_tasks"));
                } else {
                    for (Task t : tasks) {
                        String shortId = t.id.toString()
                            .substring(0, 8);
                        sender.addChatMessage(
                            new ChatComponentText(String.format("[%s] %s (%s)", shortId, t.title, t.status.name())));
                    }
                }
                break;
            }
            case "reload": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.RELOAD)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                // Re-sends each online player their team's current task list.
                List<EntityPlayerMP> online = MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList;
                for (EntityPlayerMP player : online) {
                    Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                    if (team == null) continue;
                    TaskNHNetwork.CHANNEL.sendTo(new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())), player);
                }
                sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.synced"));
                break;
            }
            case "gui": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.GUI)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (sender instanceof EntityPlayerMP) {
                    TaskNHNetwork.CHANNEL
                        .sendTo(new com.eldrinn.tasknh.network.OpenGuiPacket(), (EntityPlayerMP) sender);
                } else {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.gui_client_only"));
                }
                break;
            }
            case "create": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.CREATE)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.create"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP player)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.must_be_player"));
                    return;
                }
                Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (team == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
                    return;
                }
                String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Task task = new Task(UUID.randomUUID(), title, "", TaskStatus.OPEN);
                data.addTask(team.getTeamId(), task);
                TaskNHNetwork
                    .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "tasknh.cmd.created",
                        task.id.toString()
                            .substring(0, 8),
                        title));
                break;
            }
            case "assign": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.ASSIGN)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.assign"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.must_be_player"));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                Team senderTeam = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                if (senderTeam == null) return;
                EntityPlayerMP target = CommandBase.getPlayer(sender, args[2]);
                boolean alreadyAssigned = task.assignees.stream()
                    .anyMatch(
                        ap -> ap.playerId()
                            .equals(target.getUniqueID()));
                if (alreadyAssigned) {
                    sender.addChatMessage(
                        new ChatComponentTranslation("tasknh.cmd.already_assigned", args[2], task.title));
                    break;
                }
                task.assignees.add(new AssignedPlayer(target.getUniqueID(), System.currentTimeMillis()));
                data.updateTask(senderTeam.getTeamId(), task);
                TaskNHNetwork.sendToTeamMembers(
                    senderTeam.getMembers(),
                    new SyncAllTasksPacket(data.getTeamTasks(senderTeam.getTeamId())));
                sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.assigned_to", args[2], task.title));
                target.addChatMessage(new ChatComponentTranslation("tasknh.chat.assigned", task.title));
                break;
            }
            case "unassign": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.UNASSIGN)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.unassign"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.must_be_player"));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                Team senderTeam = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                if (senderTeam == null) return;
                EntityPlayerMP target = CommandBase.getPlayer(sender, args[2]);
                task.assignees.removeIf(
                    ap -> ap.playerId()
                        .equals(target.getUniqueID()));
                data.updateTask(senderTeam.getTeamId(), task);
                TaskNHNetwork.sendToTeamMembers(
                    senderTeam.getMembers(),
                    new SyncAllTasksPacket(data.getTeamTasks(senderTeam.getTeamId())));
                sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.unassigned_from", args[2], task.title));
                break;
            }
            case "done": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.DONE)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.done"));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                task.status = TaskStatus.DONE;
                if (sender instanceof EntityPlayerMP) {
                    Team team = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                    if (team != null) {
                        data.updateTask(team.getTeamId(), task);
                        TaskNHNetwork.sendToTeamMembers(
                            team.getMembers(),
                            new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                    }
                }
                sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.marked_done", task.title));
                break;
            }
            case "export": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.EXPORT)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                Collection<Task> tasks = getSenderTasks(sender, data);
                if (tasks == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
                    return;
                }
                Gson gson = new GsonBuilder().setPrettyPrinting()
                    .create();
                JsonArray arr = new JsonArray();
                for (Task t : tasks) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", t.id.toString());
                    obj.addProperty("title", t.title);
                    obj.addProperty("description", t.description);
                    obj.addProperty("status", t.status.name());
                    if (t.iconItem != null) obj.addProperty("iconItem", t.iconItem);
                    obj.addProperty("showOnMap", t.showOnMap);
                    if (t.location != null) {
                        JsonObject loc = new JsonObject();
                        loc.addProperty("x", t.location.x);
                        loc.addProperty("y", t.location.y);
                        loc.addProperty("z", t.location.z);
                        loc.addProperty("dimension", t.location.dimension);
                        loc.addProperty("label", t.location.label);
                        obj.add("location", loc);
                    }
                    JsonArray subtasks = new JsonArray();
                    for (com.eldrinn.tasknh.data.Subtask s : t.subtasks) {
                        JsonObject so = new JsonObject();
                        so.addProperty("title", s.title);
                        so.addProperty("checked", s.checked);
                        subtasks.add(so);
                    }
                    obj.add("subtasks", subtasks);
                    arr.add(obj);
                }
                File dir = new File(
                    MinecraftServer.getServer()
                        .getEntityWorld()
                        .getSaveHandler()
                        .getWorldDirectory(),
                    "tasknh");
                if (!dir.exists() && !dir.mkdirs()) {
                    sender.addChatMessage(
                        new ChatComponentTranslation("tasknh.cmd.export_failed", "could not create directory"));
                    return;
                }
                String filename = args.length >= 2 ? args[1] : "export";
                File out = new File(dir, filename + ".json");
                try {
                    if (!out.getCanonicalPath()
                        .startsWith(dir.getCanonicalPath() + File.separator)) {
                        sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.export_failed", "invalid path"));
                        return;
                    }
                } catch (IOException e) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.export_failed", e.getMessage()));
                    return;
                }
                try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
                    gson.toJson(arr, w);
                    sender.addChatMessage(
                        new ChatComponentTranslation("tasknh.cmd.exported", tasks.size(), out.getName()));
                } catch (IOException e) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.export_failed", e.getMessage()));
                }
                break;
            }
            case "import": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.IMPORT)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.import"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP player)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.must_be_player"));
                    return;
                }
                Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (team == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
                    return;
                }
                File dir = new File(
                    MinecraftServer.getServer()
                        .getEntityWorld()
                        .getSaveHandler()
                        .getWorldDirectory(),
                    "tasknh");
                File in = new File(dir, args[1] + ".json");
                try {
                    if (!in.getCanonicalPath()
                        .startsWith(dir.getCanonicalPath() + File.separator)) {
                        sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.file_not_found", args[1]));
                        return;
                    }
                } catch (IOException e) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.export_failed", e.getMessage()));
                    return;
                }
                if (!in.exists()) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.file_not_found", in.getName()));
                    return;
                }
                try (Reader r = new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8)) {
                    JsonArray arr = new Gson().fromJson(r, JsonArray.class);
                    int count = 0;
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        Task t = new Task(
                            UUID.randomUUID(),
                            obj.get("title")
                                .getAsString(),
                            obj.has("description") ? obj.get("description")
                                .getAsString() : "",
                            TaskStatus.valueOf(
                                obj.get("status")
                                    .getAsString()));
                        if (obj.has("iconItem")) t.iconItem = obj.get("iconItem")
                            .getAsString();
                        if (obj.has("showOnMap")) t.showOnMap = obj.get("showOnMap")
                            .getAsBoolean();
                        if (obj.has("location")) {
                            JsonObject loc = obj.getAsJsonObject("location");
                            t.location = new com.eldrinn.tasknh.data.TaskLocation(
                                loc.get("x")
                                    .getAsInt(),
                                loc.get("y")
                                    .getAsInt(),
                                loc.get("z")
                                    .getAsInt(),
                                loc.get("dimension")
                                    .getAsInt(),
                                loc.has("label") ? loc.get("label")
                                    .getAsString() : "");
                        }
                        if (obj.has("subtasks")) {
                            for (JsonElement se : obj.getAsJsonArray("subtasks")) {
                                JsonObject so = se.getAsJsonObject();
                                t.subtasks.add(
                                    new com.eldrinn.tasknh.data.Subtask(
                                        UUID.randomUUID(),
                                        so.get("title")
                                            .getAsString(),
                                        so.has("checked") && so.get("checked")
                                            .getAsBoolean()));
                            }
                        }
                        data.addTask(team.getTeamId(), t);
                        count++;
                    }
                    TaskNHNetwork.sendToTeamMembers(
                        team.getMembers(),
                        new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.imported", count));
                } catch (Exception e) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.import_failed", e.getMessage()));
                }
                break;
            }
            case "open": {
                if (!TaskNHPermissions.has(sender, TaskNHPermissions.OPEN)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.no_permission"));
                    return;
                }
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.usage.open"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP player)) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.must_be_player"));
                    return;
                }
                UUID taskId;
                try {
                    taskId = UUID.fromString(args[1]);
                } catch (IllegalArgumentException e) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.task_not_found", args[1]));
                    return;
                }
                Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (team == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
                    return;
                }
                if (data.getTask(team.getTeamId(), taskId) == null) {
                    sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.task_not_found", args[1]));
                    return;
                }
                TaskNHNetwork.CHANNEL.sendTo(new OpenGuiPacket(taskId), player);
                break;
            }
            default:
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }

    /** Returns the sender's team tasks, or null if sender is not a player / has no team. */
    private Collection<Task> getSenderTasks(ICommandSender sender, TaskNHWorldData data) {
        if (!(sender instanceof EntityPlayerMP)) return null;
        Team team = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
        if (team == null) return null;
        return data.getTeamTasks(team.getTeamId());
    }

    /** Finds a task by the first 8 chars of its UUID. Sends error to sender if not found. */
    private Task findTaskByShortId(TaskNHWorldData data, ICommandSender sender, String shortId) {
        Collection<Task> all = getSenderTasks(sender, data);
        if (all == null) {
            sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.not_in_team"));
            return null;
        }
        for (Task t : all) {
            if (t.id.toString()
                .startsWith(shortId)) return t;
        }
        sender.addChatMessage(new ChatComponentTranslation("tasknh.cmd.task_not_found", shortId));
        return null;
    }
}
