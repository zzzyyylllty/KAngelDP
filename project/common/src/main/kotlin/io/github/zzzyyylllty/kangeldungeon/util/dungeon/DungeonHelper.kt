package io.github.zzzyyylllty.kangeldungeon.util.dungeon

import com.sk89q.worldedit.function.operation.Operations
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.dataFolder
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.data.DungeonTemplate
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper.getWorldName
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/*
plugins/KAngelDP/sources/sample/
├── region/
│   ├── r.0.0.mca          # Region文件（包含地形数据）
│   ├── r.0.1.mca
│   ├── r.1.0.mca
│   └── r.1.1.mca
├── level.dat               # 世界配置文件
├── level.dat_old           # 备份
├── session.lock            # 会话锁
├── uid.dat                 # 唯一ID
└── data/                   # 世界数据
    ├── Advancements/
    ├── DimensionData/
    ├── WorldGenSettings.dat
    └── ...
*/


object DungeonHelper {

    fun getWorldName(dungeonName: String, dungeonUUID: UUID): String {
        return "KDP_${dungeonName}_$dungeonUUID"
    }

    /**
     * 通过Region文件创建副本世界
     */
    fun createDungeonWorld(
        template: DungeonTemplate,
        dungeonUUID: UUID
    ): World? {
        if (template.worldTemplate != null) {
            return createDungeonWorldByRegionFile(template, dungeonUUID)
        } else if (template.schematicFile != null) {
            return createDungeonWorldBySchematic(template, dungeonUUID)
        } else {
            severeL("ErrorTemplateOrSchematicNotExist", template.name)
        }
        return null
    }
    /**
     * 通过Region文件创建副本世界
     */
    fun createDungeonWorldByRegionFile(
        template: DungeonTemplate,
        dungeonUUID: UUID
    ): World? {
        return try {
            devLog("Creating dungeon world from template: ${template.name}")

            // 检查世界模板是否存在
            val worldTemplate = template.worldTemplate ?: run {
                severeL("ErrorWorldTemplateNotSpecified", template.name)
                return null
            }

            // val sourceFolder = File("plugins/KDungeon/templates/$worldTemplate")
            val sourceFolder = File(getDataFolder(), "dungeon/${template.name}/source")
            if (!sourceFolder.exists()) {
                severeL("ErrorTemplateNotExist", worldTemplate)
                return null
            }

            val worldName = getWorldName(template.name, dungeonUUID)
            val worldContainer = Bukkit.getWorldContainer()
            val worldFolder = File(worldContainer, worldName)

            if (!worldFolder.exists()) {
                worldFolder.mkdirs()
            }

            infoL("InfoCopyingFiles", template.name)
            copyWorldFiles(sourceFolder, worldFolder)

            val creator = WorldCreator(worldName)
                .type(WorldType.NORMAL)
                .environment(World.Environment.NORMAL)

            val world = creator.createWorld()
            world?.apply {
                difficulty = org.bukkit.Difficulty.NORMAL
                pvp = template.pvpEnabled
                isAutoSave = false

                // 应用模板配置
                setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, template.keepInventory)
                setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, template.naturalRegeneration)
            }

            infoL("SuccessWorldCreated", worldName)
            devLog("Dungeon world created: $worldName")
            world

        } catch (e: Exception) {
            severeL("ErrorWorldCreationFailed", e.message ?: "Unknown error")
            e.printStackTrace()
            null
        }
    }

    /**
     * 通过Schematic文件创建副本世界
     */
    fun createDungeonWorldBySchematic(
        template: DungeonTemplate,
        dungeonUUID: UUID
    ): World? {
        return try {
            devLog("Creating dungeon world from schematic: ${template.name}")

            // 检查Schematic文件是否存在
            val schematicFile = template.schematicFile ?: run {
                severeL("ErrorSchematicNotSpecified", template.name)
                return null
            }

            val schematicPath = File("plugins/KDungeon/schematics/$schematicFile")
            if (!schematicPath.exists()) {
                severeL("ErrorSchematicNotFound", schematicFile)
                return null
            }

            val worldName = getWorldName(template.name, dungeonUUID)
            val worldContainer = Bukkit.getWorldContainer()
            val worldFolder = File(worldContainer, worldName)

            if (!worldFolder.exists()) {
                worldFolder.mkdirs()
            }

            val creator = WorldCreator(worldName)
                .type(WorldType.FLAT)
                .environment(World.Environment.NORMAL)

            val world = creator.createWorld() ?: run {
                severeL("ErrorWorldCreationFailed", "Failed to create world: $worldName")
                return null
            }

            world.apply {
                difficulty = org.bukkit.Difficulty.NORMAL
                pvp = template.pvpEnabled
                isAutoSave = false

                // 应用模板配置
                setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, template.keepInventory)
                setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, template.naturalRegeneration)
            }

            infoL("InfoPastingSchematic", template.name)
            pasteSchematicAsync(world, schematicPath, template)

            infoL("SuccessWorldCreated", worldName)
            devLog("Dungeon world created from schematic: $worldName")
            world

        } catch (e: Exception) {
            severeL("ErrorWorldCreationFailed", e.message ?: "Unknown error")
            e.printStackTrace()
            null
        }
    }

    /**
     * 异步粘贴Schematic文件
     */
    private fun pasteSchematicAsync(
        world: World,
        schematicFile: File,
        template: DungeonTemplate
    ) {
        val plugin = Bukkit.getPluginManager().getPlugin("KAngelDP") ?: return

        submitAsync {
            try {
                devLog("Loading schematic: ${schematicFile.name}")
                val clipboard = loadSchematic(schematicFile)

                submit {
                    try {
                        pasteSchematicToWorld(world, clipboard, template)
                    } catch (e: Exception) {
                        severeL("ErrorPastingSchematic", e.message ?: "Unknown error")
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                severeL("ErrorLoadingSchematic", e.message ?: "Unknown error")
                e.printStackTrace()
            }
        }
    }

    /**
     * 加载Schematic文件
     */
    private fun loadSchematic(file: File): com.sk89q.worldedit.extent.clipboard.Clipboard {
        val format = com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file)
            ?: throw IllegalArgumentException("Unsupported schematic format: ${file.name}")

        return file.inputStream().use { input ->
            format.getReader(input).read()
        }
    }

    /**
     * 将Schematic粘贴到世界
     */
    private fun pasteSchematicToWorld(
        world: World,
        clipboard: com.sk89q.worldedit.extent.clipboard.Clipboard,
        template: DungeonTemplate
    ) {
        try {
            val adapter = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)
            val editSession = com.sk89q.worldedit.WorldEdit.getInstance()
                .newEditSession(adapter)

            editSession.use { session ->
                val holder = com.sk89q.worldedit.session.ClipboardHolder(clipboard)

                // 使用模板中的出生点作为粘贴位置
                val pasteLocation = com.sk89q.worldedit.math.BlockVector3.at(
                    template.spawnPoint.blockX,
                    template.spawnPoint.blockY,
                    template.spawnPoint.blockZ
                )

                val operation = holder.createPaste(session)
                    .to(pasteLocation)
                    .ignoreAirBlocks(false)
                    .build()

                Operations.complete(operation)
            }

            infoL("SuccessSchematicPasted", world.name)
            devLog("Schematic pasted successfully to world: ${world.name}")
        } catch (e: Exception) {
            severeL("ErrorPastingSchematic", e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 复制世界文件（Region + level.dat等）
     */
    private fun copyWorldFiles(source: File, target: File) {
        try {
            // 复制region文件夹
            val sourceRegion = File(source, "region")
            if (sourceRegion.exists()) {
                val targetRegion = File(target, "region")
                targetRegion.mkdirs()

                sourceRegion.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".mca")) {
                        try {
                            Files.copy(
                                file.toPath(),
                                File(targetRegion, file.name).toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                            devLog("Copied region file: ${file.name}")
                        } catch (e: Exception) {
                            warningL("WarningCopyRegionFailed", file.name, e.message ?: "Unknown error")
                        }
                    }
                }
            } else {
                warningL("WarningRegionFolderNotFound", source.name)
            }

            // 复制level.dat
            val sourceLevelDat = File(source, "level.dat")
            if (sourceLevelDat.exists()) {
                try {
                    Files.copy(
                        sourceLevelDat.toPath(),
                        File(target, "level.dat").toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    devLog("Copied level.dat")
                } catch (e: Exception) {
                    warningL("WarningCopyFileFailed", "level.dat", e.message ?: "Unknown error")
                }
            }

            // 复制其他必要文件
            listOf("level.dat_old", "session.lock", "uid.dat", "data").forEach { fileName ->
                val sourceFile = File(source, fileName)
                if (sourceFile.exists()) {
                    try {
                        if (sourceFile.isDirectory) {
                            copyDirectory(sourceFile, File(target, fileName))
                        } else {
                            Files.copy(
                                sourceFile.toPath(),
                                File(target, fileName).toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                        devLog("Copied file/folder: $fileName")
                    } catch (e: Exception) {
                        warningL("WarningCopyFileFailed", fileName, e.message ?: "Unknown error")
                    }
                }
            }
        } catch (e: Exception) {
            severeL("ErrorCopyingWorldFiles", e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 递归复制目录
     */
    private fun copyDirectory(source: File, target: File) {
        if (!target.exists()) {
            target.mkdirs()
        }

        source.listFiles()?.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                copyDirectory(file, targetFile)
            } else {
                Files.copy(
                    file.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    /**
     * 卸载并删除副本世界
     */
    fun unloadDungeonWorld(dungeonInstance: DungeonInstance) {
        val worldName = dungeonInstance.worldName
        val world = dungeonInstance.world

        if (world != null) {
            devLog("Unloading dungeon world: $worldName")

            // 传送所有玩家离开
            world.players.forEach { player ->
                try {
                    player.teleport(Bukkit.getWorlds()[0].spawnLocation)
                } catch (e: Exception) {
                    warningL("WarningTeleportFailed", player.name, e.message ?: "Unknown error")
                }
            }

            // 卸载世界
            Bukkit.unloadWorld(world, false)
        }

        // 异步删除文件
        val plugin = Bukkit.getPluginManager().getPlugin("KAngelDP") ?: return
        submitAsync {
            try {
                val worldFolder = File(Bukkit.getWorldContainer(), worldName)
                deleteDirectory(worldFolder)
                infoL("SuccessWorldUnloaded", worldName)
                devLog("Dungeon world deleted: $worldName")
            } catch (e: Exception) {
                severeL("ErrorDeletingWorld", e.message ?: "Unknown error")
                e.printStackTrace()
            }
        }
    }

    /**
     * 递归删除目录
     */
    private fun deleteDirectory(directory: File): Boolean {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                deleteDirectory(file)
            }
        }
        return directory.delete()
    }

    /**
     * 检查副本世界是否存在
     */
    fun dungeonWorldExists(dungeonInstance: DungeonInstance): Boolean {
        return Bukkit.getWorld(dungeonInstance.worldName) != null
    }

    fun createDungeon(templateName: String, players: Collection<Player>, leader: Player, meta: Map<String, Any>) {
        val uuid = UUID.randomUUID()
        val template = KAngelDungeon.dungeonTemplates[templateName] ?: run {
            severeL("ErrorTemplateNotExist", templateName)
            return
        }
        val worldName = DungeonHelper.getWorldName(templateName, uuid)
        devLog("Create dungeon $templateName")
        devLog("Creating world $worldName")

        val world = createDungeonWorld(template, uuid) ?:run {
            devLog("Dungeon world creation is null, skipped")
            return
        }
        val instance = DungeonInstance(
            templateName = templateName,
            uuid = uuid,
            players = players.map { it.uniqueId }.toMutableSet(),
            deadPlayers = mutableSetOf(),
            leaderUUID = leader.uniqueId,
            startedAt = null,
            completedAt = null,
            state = DungeonState.PREPARING,
            meta = finalMeta,
            spawnLocation = template.spawnVector.toLocation(world)
        )
        KAngelDungeon.dungeonInstances[uuid] = instance
    }
}
}