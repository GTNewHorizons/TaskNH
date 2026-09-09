# TaskNH

A task management mod for Minecraft 1.7.10 (GregTech: New Horizons). TaskNH lets teams create, assign, and track tasks directly in-game, with a GUI, map markers, and a full command interface.

| Task List                               | Task Detail                                     |
|-----------------------------------------|-------------------------------------------------|
| ![Task List](img/themes_comparison.png) | ![Task Detail](img/themes_comparison_detail.png) | 

## Features

- **Team-scoped tasks**: each GTNHLib team has its own isolated task list; all members stay in sync in real time
- **Three-status workflow**: To do / Doing / Done, switchable from the task detail panel
- **Subtasks**: attach child tasks to a task, one nesting level deep; a subtask is a full task with its own status, assignees and location. Done subtasks drop out of the list until you ask for them
- **Manual order**: drag a task to move it, with its subtasks; arrows reorder a subtask under its parent. The whole team sees the same order
- **Checklist**: add checklist items to any task; check them off individually
- **HUD**: pin up to 5 tasks to an on-screen HUD with their checklists; position, scale and background are configurable
- **Item tracking**: drag an item onto the task's tracking slot and the task closes itself once a team member carries it, in the amount the slot asks for
- **Assignees**: assign any online player to a task from the GUI or via command, and nudge one of them with a reminder in chat
- **Map markers**: pin a world coordinate to a task and display it on the map (requires Navigator)
- **Task icon**: set any item as a task icon by dragging it from NEI onto the icon slot; right-click to clear, left-click to open its recipes
- **Export / import**: dump a team's tasks to a JSON file and reload them on another world or share them
- **Dark and light themes**: toggle with the sun button in the bottom-right of the GUI
- **Search**: expandable search bar filters the task list live by title or description
- **Quest import**: create a task from a BetterQuesting quest via the quest context menu, with required items turned into subtasks
- **Permissions**: every subcommand has its own permission node, tunable through ServerUtilities ranks

## Requirements

| Dependency                       | Version        |
|----------------------------------|----------------|
| Minecraft Forge                  | 1.7.10         |
| GTNHLib                          | 0.11.47+       |
| ModularUI2                       | 2.3.88-1.7.10+ |
| Navigator *(optional)*           | 1.1.9+         |
| NotEnoughItems GTNH *(optional)* | 2.8.105-GTNH+  |
| BetterQuesting *(optional)*      | 3.8.84-GTNH+   |
| ServerUtilities *(optional)*     | 2.4.9+         |

Navigator is only required for map marker support. Without it the mod works normally and the Location section is still available for storing coordinates.

NEI is only required for the item slots: dragging an item onto one, opening its recipes with a left click, and the R and U hotkeys over it. Without NEI the slots stay visible and hold whatever a task already carries, but you cannot drop anything on them.

BetterQuesting is only required for the quest context menu entry. Older BetterQuesting versions without the context menu API are detected on load and the integration is skipped.

ServerUtilities is only required for rank-based permissions. Without it, `reload`, `export` and `import` require OP and every other subcommand is open to all players.

## Installation

1. Drop the TaskNH `.jar` into your `mods/` folder.
2. Make sure GTNHLib and ModularUI2 are also present.
3. Optionally add Navigator for map marker support.

## Opening the GUI

- Press **Y** (default keybind, rebindable under Controls) to open the TaskNH window for your team
- Run `/tasknh gui` from chat

## Commands

Access is controlled per subcommand by the `tasknh.<subcommand>` permission nodes. Without ServerUtilities, `reload`, `export` and `import` require OP and the rest are available to any player.

```
/tasknh <subcommand>
```

| Subcommand               | Description                                            |
|--------------------------|--------------------------------------------------------|
| `list`                   | List all tasks for your team                           |
| `create <title>`         | Create a new task with the given title                 |
| `assign <id> <player>`   | Assign a player to a task                              |
| `unassign <id> <player>` | Remove a player from a task                            |
| `done <id>`              | Mark a task as Done                                    |
| `export [name]`          | Export all team tasks to `<world>/tasknh/<name>.json` |
| `import <name>`          | Import tasks from `<world>/tasknh/<name>.json`        |
| `reload`                 | Re-sync tasks to all online players                    |
| `gui`                    | Open the GUI (player only)                             |
| `open <uuid>`            | Open the GUI on a specific task (player only)          |

`<id>` is the first 8 characters of a task UUID, shown in `/tasknh list` and in the GUI.

## GUI Overview

The window has two pages. Click a task row to open its detail page; use the back button (←) to return to the list.

**Page 1 - task list**
- Three tabs across the top filter by status: To do / Doing / Done
- The search button (magnifier icon) expands a live search field; click again to collapse and clear
- Drag a task by its row to move it up or down; its subtasks travel with it and the new order reaches the whole team. A subtask moves among its siblings with the arrows on its row
- A task with done subtasks gets an eye button left of the star; its tooltip counts them and a click brings them back
- `+ New Task` opens a blank create form
- The buttons in the bottom-right open the HUD position settings and toggle the theme

**Page 2 - task detail**
- **Icon slot** (top-left): drag any item from NEI onto the slot to set it as the task icon; right-click to clear, left-click to open its recipes in NEI, and the R and U hotkeys work over the slot
- **Title field**: editable inline, up to 256 characters
- **Delete button** (top-right): permanently deletes the task for the whole team
- **Pin button** (top-right): pins the task to the HUD, up to 5 at a time; subtasks cannot be pinned
- **Description**: free-text field, up to 512 characters
- **Status**: toggle between To do / Doing / Done
- **Assignees**: click any online player to assign or unassign them; the picker stays closed until the task has a title. Each assignee gets a `Remind` button that sends them a chat message with a link to the task
- **Location**: X/Y/Z coordinate fields; `Pos` button captures your current position; `Show on map` toggle controls the Navigator marker
- **Auto-complete on item**: drag an item from NEI onto the slot; the task completes once one team member carries it. The amount comes from the quantity field in NEI and shows in the corner of the slot; middle-click the slot to type it instead. It tops out at 2304, a full inventory of stacks of 64. An item counts when its id, meta and internal name match the one on the slot, so two CropsNH seeds of different plants stay apart while their growth values are ignored, and an unanalyzed seed carries no plant name and waits for a scan. A mod that folds a changing state into that name, a tool mode for instance, makes the item stop counting once the player switches it
- **Subtasks**: child tasks of this task; click one to open it, use `Parent:` at the top of a subtask to go back. A subtask has no subtasks of its own
- **Checklist**: check off items or remove them; add new ones with the `+` button

The search bar, the subtask and checklist add fields and the X/Y/Z fields hold up to 256 characters each.

## HUD

Pinned tasks are drawn on screen with their checklists. Open the HUD settings from the button in the bottom-right of the task list: drag the handle to reposition, adjust scale, background and how many tasks and checklist lines are shown, or turn the HUD off. Pins and HUD settings are per client and stored in the mod config folder.

## Config

The config is server-side. Item tracking sits under the `item_tracking` category:

| Option    | Default | Description                                                                       |
|-----------|---------|-----------------------------------------------------------------------------------|
| `enabled` | `true`  | Auto-complete a task when its tracked item appears in a team member's inventory     |
| `announce`| `true`  | Send a chat message to the team when a task auto-completes                          |

Reminders sit under the `reminders` category:

| Option     | Default | Range    | Description                                                                    |
|------------|---------|----------|--------------------------------------------------------------------------------|
| `cooldown` | `60`    | 0 - 3600 | Seconds before the same player can be reminded of the same task again; 0 removes the wait |

## Export / Import format

Tasks are stored as a JSON array. Each object supports the following fields:

```json
[
  {
    "id": "xxxxxxxx-...",
    "title": "Build the smeltery",
    "description": "Use steel casing, not iron.",
    "status": "OPEN",
    "iconStack": "{id:\"tconstruct:smeltery_controller\",Count:1b,Damage:0s}",
    "trackStack": "{id:\"minecraft:brick\",Count:1b,Damage:0s}",
    "trackItemCount": 64,
    "showOnMap": true,
    "location": { "x": 100, "y": 64, "z": -200, "dimension": 0, "label": "" },
    "checklist": [
      { "title": "Gather firebricks", "checked": false }
    ]
  }
]
```

Valid `status` values: `OPEN`, `IN_PROGRESS`, `DONE`.

`checklist` was named `subtasks` before subtasks became real tasks; the old key is still accepted on import.

`iconStack` and `trackStack` hold the item as NBT text, so items that differ only by NBT stay apart. They were `iconItem` and `trackItem`, a `modid:item:meta` string, and those keys are still accepted on import. An item tag holding a byte array or a quoted string does not survive the round trip, since NBT text has no escaping for either.

`trackItemCount` is how many of `trackStack` a member has to carry. The export leaves it out when the task asks for one, and an import clamps it to 1 - 2304.

Assignees, subtask relations and the manual order stay out of the export. Imported tasks come back as unassigned root tasks at the end of their tab.

Files are saved to and loaded from `<world save>/tasknh/`. The `.json` extension is added automatically.

## License

MIT
