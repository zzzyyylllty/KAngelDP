package io.github.zzzyyylllty.kangeldungeon.editor

enum class EditorCategory(val key: String, val displayName: String) {
    SETTINGS("settings", "Dungeon Settings"),
    MONSTER("monster", "Monster Groups"),
    OBSTACLE("obstacle", "Obstacles"),
    REGION("region", "Regions"),
    INTERACT("interact", "Interactions"),
    PLAN("plan", "Plans"),
    KIT("kit", "Kits"),
    TASK("task", "Tasks"),
    LOOT("loot", "Loot Chests"),
    DIFFICULTY("difficulty", "Difficulty Presets");
}
