# TaskNH

A task management mod for Minecraft 1.7.10 (GregTech: New Horizons). TaskNH lets teams create, assign, and track tasks directly in-game, with a GUI, map markers, and a full command interface.

| Task List                               | Task Detail                                     |
|-----------------------------------------|-------------------------------------------------|
| ![Task List](img/themes_comparison.png) | ![Task Detail](img/themes_comparison_detail.png) | 

## Features

- **Team-scoped tasks**: each GTNHLib team has its own isolated task list; all members stay in sync in real time
- **Three-status workflow**: To do / Doing / Done, switchable from the task detail panel
- **Subtasks**: attach child tasks to a task, one nesting level deep; a subtask is a full task with its own status, assignees and location. Fold the done subtasks or all of them under their parent
- **Manual order**: drag a task to move it, with its subtasks; arrows reorder a subtask under its parent. The whole team sees the same order
- **Checklist**: add checklist items to any task; check them off individually
- **HUD**: pin up to 5 tasks to an on-screen HUD with their checklists, with the tracked item's icon and how many you carry; position, scale and background are configurable
- **Item tracking**: drag an item onto the task's tracking slot and the task closes itself once a team member carries it, in the amount the slot asks for
- **Assignees**: assign any online player to a task from the GUI or via command, and nudge one of them with a reminder in chat
- **Map markers**: pin a world coordinate to a task and display it on the map (requires Navigator)
- **Task icon**: set any item as a task icon by dragging it from NEI onto the icon slot; right-click to clear, left-click to open its recipes
- **Export / import**: dump a team's tasks to a JSON file and reload them on another world or share them
- **Dark and light themes**: toggle with the sun button in the bottom-right of the GUI
- **Search**: expandable search bar filters the task list live by title or description
- **Quest import**: create a task from a BetterQuesting quest via the quest context menu, with required items turned into checklist items that check themselves once a team member carries the item
- **Multiblock import**: the + button on a multiblock's structure page in NEI creates a task with the controller as its icon and each part as a tracked checklist item, counted for the tier and channels you picked. Hatches are left out, since any tier fits their slot
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
| BlockRenderer6343 *(optional)*   | 1.4.21+        |
| ServerUtilities *(optional)*     | 2.4.9+         |

Navigator is only required for map marker support. Without it the mod works normally and the Location section is still available for storing coordinates.

NEI is only required for the item slots: dragging an item onto one, opening its recipes with a left click, and the R and U hotkeys over it. Without NEI the slots stay visible and hold whatever a task already carries, but you cannot drop anything on them.

BetterQuesting is only required for the quest context menu entry. Older BetterQuesting versions without the context menu API are detected on load and the integration is skipped.

BlockRenderer6343 is only required for the multiblock button, which also needs NEI. Set the preview's Layer to All before pressing it, since a single layer lists only its own parts.

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
- A task with subtasks gets an arrow button left of the star. A click cycles through all shown, done hidden and all hidden, skipping the middle step when it changes nothing; the tooltip names the current state and counts the done subtasks. Each task keeps its choice after a restart
- `+ New Task` opens a short form with the icon, title and description. `Create Task` or Enter in the title creates the task and opens the full form; the button stays dimmed until the title has text
- The buttons in the bottom-right open the HUD position settings and toggle the theme

**Page 2 - task detail**
- **Icon slot** (top-left): drag any item from NEI onto the slot to set it as the task icon; right-click to clear, left-click to open its recipes in NEI, and the R and U hotkeys work over the slot
- **Title field**: editable inline, up to 256 characters
- **Delete button** (top-right): permanently deletes the task for the whole team
- **Pin button** (top-right): pins the task to the HUD, up to 5 at a time; subtasks cannot be pinned
- **Description**: free-text field, up to 512 characters
- **Status**: toggle between To do / Doing / Done
- **Assignees**: click any online player to assign or unassign them. Each assignee gets a `Remind` button that sends them a chat message with a link to the task. When nobody else is in your team, a hint points to `/gtnhteam invite`. A singleplayer world that isn't open to LAN hides the section, unless the task already has assignees
- **Location**: X/Y/Z coordinate fields; `Pos` button captures your current position; `Show on map` toggle controls the Navigator marker
- **Auto-complete on item**: drag an item from NEI onto the slot; the task completes once one team member carries it. The amount comes from the quantity field in NEI and shows in the corner of the slot; middle-click the slot to type it instead. The field takes expressions such as `64*8` or `2k`, right-click clears it, and leaving it empty keeps the old amount. It tops out at 2304, a full inventory of stacks of 64. An item counts when its id, meta and internal name match the one on the slot, so two CropsNH seeds of different plants stay apart while their growth values are ignored, and an unanalyzed seed carries no plant name and waits for a scan. A mod that folds a changing state into that name, a tool mode for instance, makes the item stop counting once the player switches it
- **Subtasks**: child tasks of this task; click one to open it, use `Parent:` at the top of a subtask to go back. A subtask has no subtasks of its own
- **Checklist**: check off items or remove them; add new ones with the `+` button. With Auto-done on, the task completes once every item is checked; unchecking one later does not reopen it. Each item has its own item slot that works like Auto-complete on item and checks the item instead of completing the task; the player who carries it gets a chat message. An item imported from a quest that asks for an OreDictionary tag accepts any item under that tag, and setting a slot by hand drops the tag

The search bar, the subtask and checklist add fields and the X/Y/Z fields hold up to 256 characters each.

## HUD

Pinned tasks are drawn on screen with their checklists. A task or checklist item with a tracked item shows its icon and how many of it you carry, like 5/10. The count covers only your own inventory, since the mod checks each member's inventory on its own. Tasks in progress come first, then open and done ones, each group in the order of the task list. Open the HUD settings from the button in the bottom-right of the task list: drag the handle to reposition, adjust scale, background and how many tasks and checklist lines are shown, or turn the HUD off. The client keeps pins and subtask folds separately for each world and shares the HUD settings between them, all in the mod config folder.

## Config

The config is server-side. Item tracking sits under the `item_tracking` category:

| Option    | Default | Description                                                                       |
|-----------|---------|-----------------------------------------------------------------------------------|
| `enabled` | `true`  | Auto-complete a task or check a checklist item when its tracked item appears in a team member's inventory |
| `announce`| `true`  | Send a chat message to the team when a task auto-completes, and to the player when a checklist item gets checked |

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
    "completeOnChecklist": true,
    "showOnMap": true,
    "location": { "x": 100, "y": 64, "z": -200, "dimension": 0, "label": "" },
    "checklist": [
      { "title": "Gather firebricks", "checked": false },
      { "title": "64x Glass", "checked": false, "trackStack": "{id:\"minecraft:glass\",Count:1b,Damage:0s}", "trackItemCount": 64, "trackOre": "blockGlass" }
    ]
  }
]
```

Valid `status` values: `OPEN`, `IN_PROGRESS`, `DONE`.

`checklist` was named `subtasks` before subtasks became real tasks; the old key is still accepted on import.

`iconStack` and `trackStack` hold the item as NBT text, so items that differ only by NBT stay apart. They were `iconItem` and `trackItem`, a `modid:item:meta` string, and those keys are still accepted on import. An item tag holding a byte array or a quoted string does not survive the round trip, since NBT text has no escaping for either.

`completeOnChecklist` closes the task once every checklist item is checked. The export leaves it out when it is off.

A checklist item takes the same `trackStack` and `trackItemCount` as a task, plus `trackOre`, an OreDictionary name that any item under it satisfies. All three are optional.

`trackItemCount` is how many of `trackStack` a member has to carry. The export leaves it out when the task asks for one, and an import clamps it to 1 - 2304.

Assignees, subtask relations and the manual order stay out of the export. Imported tasks come back as unassigned root tasks at the end of their tab.

Files are saved to and loaded from `<world save>/tasknh/`. The `.json` extension is added automatically.

## License

MIT
