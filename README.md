# TaskNH

A task management mod for Minecraft 1.7.10 (GregTech: New Horizons). TaskNH lets teams create, assign, and track tasks directly in-game, with a GUI, map markers, and a full command interface.

| Task List                               | Task Detail                                     |
|-----------------------------------------|-------------------------------------------------|
| ![Task List](img/themes_comparison.png) | ![Task Detail](img/themes_comparison_detail.png) | 

## Features

- **Team-scoped tasks**: each GTNHLib team has its own isolated task list; all members stay in sync in real time
- **Three-status workflow**: To do / Doing / Done, switchable from the task detail panel
- **Subtasks**: add checklist items to any task; check them off individually
- **Assignees**: assign any online player to a task from the GUI or via command
- **Map markers**: pin a world coordinate to a task and display it on the map (requires Navigator)
- **Task icon**: set any item as a task icon by dragging it from NEI onto the icon slot; right-click to clear
- **Export / import**: dump a team's tasks to a JSON file and reload them on another world or share them
- **Dark and light themes**: toggle with the sun button in the bottom-right of the GUI
- **Search**: expandable search bar filters the task list live by title or description

## Requirements

| Dependency                       | Version        |
|----------------------------------|----------------|
| Minecraft Forge                  | 1.7.10         |
| GTNHLib                          | 0.10.3+        |
| ModularUI2                       | 2.3.66-1.7.10+ |
| Navigator *(optional)*           | 1.1.3+         |
| NotEnoughItems GTNH *(optional)* | 2.8.93-GTNH+   |

Navigator is only required for map marker support. Without it the mod works normally and the Location section is still available for storing coordinates.

NEI is only required for the icon ghost slot. Without it the icon slot is visible but cannot be interacted with.

## Installation

1. Drop the TaskNH `.jar` into your `mods/` folder.
2. Make sure GTNHLib and ModularUI2 are also present.
3. Optionally add Navigator for map marker support.

## Opening the GUI

- Press **Y** (default keybind, rebindable under Controls) to open the TaskNH window for your team
- Run `/tasknh gui` from chat

## Commands

All commands are available to any player. The `reload` subcommand requires OP.

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
| `reload`                 | Re-sync tasks to all online players (OP only)          |
| `gui`                    | Open the GUI (player only)                             |

`<id>` is the first 8 characters of a task UUID, shown in `/tasknh list` and in the GUI.

## GUI Overview

The window has two pages. Click a task row to open its detail page; use the back button (←) to return to the list.

**Page 1 - task list**
- Three tabs across the top filter by status: To do / Doing / Done
- The search button (magnifier icon) expands a live search field; click again to collapse and clear
- `+ New Task` opens a blank create form
- The sun/moon button in the bottom-right toggles the theme

**Page 2 - task detail**
- **Icon slot** (top-left): drag any item from NEI onto the slot to set it as the task icon; right-click to clear
- **Title field**: editable inline
- **Delete button** (top-right): permanently deletes the task for the whole team
- **Description**: free-text field
- **Status**: toggle between To do / Doing / Done
- **Assignees**: click any online player to assign or unassign them
- **Location**: X/Y/Z coordinate fields; `Pos` button captures your current position; `Show on map` toggle controls the Navigator marker
- **Subtasks**: check off items or remove them; add new ones with the `+` button

## Export / Import format

Tasks are stored as a JSON array. Each object supports the following fields:

```json
[
  {
    "id": "xxxxxxxx-...",
    "title": "Build the smeltery",
    "description": "Use steel casing, not iron.",
    "status": "OPEN",
    "iconItem": "tconstruct:smeltery_controller:0",
    "showOnMap": true,
    "location": { "x": 100, "y": 64, "z": -200, "dimension": 0, "label": "" },
    "subtasks": [
      { "title": "Gather firebricks", "checked": false }
    ]
  }
]
```

Valid `status` values: `OPEN`, `IN_PROGRESS`, `DONE`.

Files are saved to and loaded from `<world save>/tasknh/`. The `.json` extension is added automatically.

## License

MIT
