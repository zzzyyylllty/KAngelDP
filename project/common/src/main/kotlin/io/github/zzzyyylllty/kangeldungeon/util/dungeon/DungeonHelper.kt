package io.github.zzzyyylllty.kangeldungeon.util.dungeon

import com.sk89q.worldedit.function.operation.Operations
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.dataFolder
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonMeta
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.data.DungeonTemplate
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper.getWorldName
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.event.SubscribeEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    /**
     * 缓存玩家进入地牢前的位置，用于离开时传送回去
     */
    val playerPreviousLocations = ConcurrentHashMap<UUID, Location>()

    fun getWorldName(dungeonName: String, dungeonUUID: UUID): String {
        return "KDP_${dungeonName}_$dungeonUUID"
    }

    /**
     * 查找世界模板源文件夹
     * 优先使用配置的 worldTemplate，找不到则回退到 templateName 作为文件夹名
     */
    private fun resolveSourceFolder(worldTemplate: String, templateName: String): File? {
        val primary = File(getDataFolder(), "sources/$worldTemplate")
        if (primary.exists()) return primary

        if (worldTemplate != templateName) {
            val fallback = File(getDataFolder(), "sources/$templateName")
            if (fallback.exists()) {
                devLog("World template '$worldTemplate' not found, falling back to '$templateName'")
                return fallback
            }
        }
        return null
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

            val sourceFolder = resolveSourceFolder(worldTemplate, template.name) ?: run {
                severeL("ErrorTemplateNotExist", worldTemplate)
                return null
            }

            val worldName = getWorldName(template.name, dungeonUUID)
            val worldContainer = Bukkit.getWorldContainer()
            val worldFolder = File(worldContainer, worldName)

            // 检查世界是否已存在（可能来自未清理干净的旧实例）
            val existingWorld = Bukkit.getWorld(worldName)
            if (existingWorld != null) {
                warningL("WarningWorldAlreadyLoaded", worldName)
                Bukkit.unloadWorld(existingWorld, false)
            }

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
     * 通过Schematic文件创建副本世界（仅创建平坦世界，不粘贴Schematic）
     * Schematic的异步粘贴由 [createDungeon] 在实例创建后发起
     */
    fun createDungeonWorldBySchematic(
        template: DungeonTemplate,
        dungeonUUID: UUID
    ): World? {
        return try {
            devLog("Creating dungeon world from schematic: ${template.name}")

            val schematicFile = template.schematicFile ?: run {
                severeL("ErrorSchematicNotSpecified", template.name)
                return null
            }

            val schematicPath = File(getDataFolder(), "schematics/$schematicFile")
            if (!schematicPath.exists()) {
                severeL("ErrorSchematicNotFound", schematicFile)
                return null
            }

            val worldName = getWorldName(template.name, dungeonUUID)
            val worldContainer = Bukkit.getWorldContainer()
            val worldFolder = File(worldContainer, worldName)

            // 检查世界是否已存在（可能来自未清理干净的旧实例）
            val existingWorld = Bukkit.getWorld(worldName)
            if (existingWorld != null) {
                warningL("WarningWorldAlreadyLoaded", worldName)
                Bukkit.unloadWorld(existingWorld, false)
            }

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
                setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, template.keepInventory)
                setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, template.naturalRegeneration)
            }

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
     * 异步粘贴Schematic文件，完成后回调
     * @param onSuccess 粘贴成功时在主线程回调
     * @param onFailure 粘贴失败时在主线程回调（需清理地牢）
     */
    private fun pasteSchematicAsync(
        world: World,
        schematicFile: File,
        template: DungeonTemplate,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        submitAsync {
            try {
                devLog("Loading schematic: ${schematicFile.name}")
                val clipboard = loadSchematic(schematicFile)

                submit {
                    try {
                        pasteSchematicToWorld(world, clipboard, template)
                        onSuccess?.invoke()
                    } catch (e: Exception) {
                        severeL("ErrorPastingSchematic", e.message ?: "Unknown error")
                        e.printStackTrace()
                        onFailure?.invoke(e.message ?: "Unknown error")
                    }
                }
            } catch (e: Exception) {
                severeL("ErrorLoadingSchematic", e.message ?: "Unknown error")
                e.printStackTrace()
                submit {
                    onFailure?.invoke(e.message ?: "Unknown error")
                }
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
        val adapter = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)
        val editSession = com.sk89q.worldedit.WorldEdit.getInstance()
            .newEditSession(adapter)

        editSession.use { session ->
            val holder = com.sk89q.worldedit.session.ClipboardHolder(clipboard)

            // 使用模板中的出生点作为粘贴位置
            val pasteLocation = com.sk89q.worldedit.math.BlockVector3.at(
                template.schematicPasteLocation.blockX,
                template.schematicPasteLocation.blockY,
                template.schematicPasteLocation.blockZ
            )

            val operation = holder.createPaste(session)
                .to(pasteLocation)
                .ignoreAirBlocks(false)
                .build()

            Operations.complete(operation)
        }

        infoL("SuccessSchematicPasted", world.name)
        devLog("Schematic pasted successfully to world: ${world.name}")
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
                    if (file.isFile && (file.name.endsWith(".mca") || file.name.endsWith(".mcc") || file.name.endsWith(".mcr"))) {
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
     * @param syncDelete 是否同步删除世界文件夹（关闭服务器时应为 true，确保文件夹被清理）
     */
    fun unloadDungeonWorld(dungeonInstance: DungeonInstance, syncDelete: Boolean = false) {
        val worldName = dungeonInstance.worldName
        val world = dungeonInstance.world

        if (world != null) {
            devLog("Unloading dungeon world: $worldName")

            // 传送所有玩家返回进入地牢前的位置
            world.players.forEach { player ->
                try {
                    val prev = playerPreviousLocations.remove(player.uniqueId)
                    if (prev != null) {
                        player.teleport(prev)
                    }
                } catch (e: Exception) {
                    warningL("WarningTeleportFailed", player.name, e.message ?: "Unknown error")
                }
            }

            // 还原障碍物方块并清理追踪数据（需要在世界卸载前执行）
            ObstacleManager.restoreBlocks(dungeonInstance)

            // 卸载世界
            Bukkit.unloadWorld(world, false)
        }

        // 清理所有地牢玩家的位置缓存（包括已离线的）
        dungeonInstance.players.forEach { uuid ->
            playerPreviousLocations.remove(uuid)
        }

        // 清理怪物追踪数据
        MonsterManager.clearWorld(worldName)

        // 删除世界文件夹（确保文件夹删除后再从活跃实例中移除，防止插件重载时文件夹残留）
        val deleteTask = {
            try {
                val worldFolder = File(Bukkit.getWorldContainer(), worldName)
                if (worldFolder.exists()) {
                    deleteDirectory(worldFolder)
                    infoL("SuccessWorldUnloaded", worldName)
                    devLog("Dungeon world deleted: $worldName")
                }
            } catch (e: Exception) {
                severeL("ErrorDeletingWorld", e.message ?: "Unknown error")
                e.printStackTrace()
            } finally {
                // 无论删除成功与否，都从活跃实例中移除
                KAngelDungeon.dungeonInstances.remove(dungeonInstance.uuid)
                KAngelDungeon.worldInstanceIndex.remove(worldName)
            }
        }

        if (syncDelete) {
            deleteTask()
        } else {
            // 避免重复提交清理任务：如果实例已从 dungeonInstances 移除则跳过
            if (KAngelDungeon.dungeonInstances.containsKey(dungeonInstance.uuid)) {
                submitAsync { deleteTask() }
            }
        }
    }

    /**
     * 递归删除目录（含重试机制，解决 Windows 文件句柄延迟释放问题）
     */
    private fun deleteDirectory(directory: File, maxRetries: Int = 5): Boolean {
        if (!directory.exists()) return true

        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                deleteDirectory(file, maxRetries)
            }
        }

        var retries = 0
        while (retries < maxRetries) {
            if (directory.delete()) return true
            retries++
            if (retries < maxRetries) {
                try {
                    Thread.sleep(200)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        if (directory.exists()) {
            warningL("WarningDeleteDirectoryFailed", directory.name)
        }
        return !directory.exists()
    }

    /**
     * 检查副本世界是否存在
     */
    fun dungeonWorldExists(dungeonInstance: DungeonInstance): Boolean {
        return Bukkit.getWorld(dungeonInstance.worldName) != null
    }

    fun createDungeon(
        templateName: String,
        players: Collection<Player>,
        leader: Player,
        meta: Map<String, Any>,
        onWorldReady: ((UUID) -> Unit)? = null
    ): UUID? {
        val uuid = UUID.randomUUID()
        val template = KAngelDungeon.dungeonTemplates[templateName] ?: run {
            severeL("ErrorTemplateNotExist", templateName)
            return null
        }
        devLog("Create dungeon $templateName")

        // 缓存所有玩家进入地牢前的位置
        players.forEach { player ->
            playerPreviousLocations[player.uniqueId] = player.location.clone()
        }

        // 确保世界创建在主线程执行（WorldCreator.createWorld() 必须运行在主线程）
        val createWorldAndInstance: () -> Unit = {
            val world = createDungeonWorld(template, uuid)
            if (world != null) {
                completeDungeonCreation(template, uuid, world, players, leader, meta, onWorldReady)
            } else {
                devLog("Dungeon world creation is null, skipped")
                players.forEach { playerPreviousLocations.remove(it.uniqueId) }
            }
        }

        if (Bukkit.isPrimaryThread()) {
            createWorldAndInstance()
        } else {
            submit { createWorldAndInstance() }
        }

        return uuid
    }

    /**
     * 在世界创建完成后完成地牢实例的初始化（必须在主线程调用）
     */
    private fun completeDungeonCreation(
        template: DungeonTemplate,
        uuid: UUID,
        world: World,
        players: Collection<Player>,
        leader: Player,
        meta: Map<String, Any>,
        onWorldReady: ((UUID) -> Unit)?
    ) {
        val worldName = DungeonHelper.getWorldName(template.name, uuid)
        val finalMeta = meta.toMutableMap()
        finalMeta["name"] = template.name
        finalMeta["template"] = template
        val dungeonMeta = DungeonMeta(ConcurrentHashMap(finalMeta))

        val playerUUIDs = ConcurrentHashMap.newKeySet<UUID>()
        playerUUIDs.addAll(players.map { it.uniqueId })
        val isSchematicMode = template.schematicFile != null
        val instance = DungeonInstance(
            templateName = template.name,
            uuid = uuid,
            players = playerUUIDs,
            deadPlayers = ConcurrentHashMap.newKeySet(),
            leaderUUID = leader.uniqueId,
            startedAt = null,
            completedAt = null,
            state = DungeonState.PREPARING,
            meta = dungeonMeta,
            spawnLocation = template.effectiveSpawnpoint.toLocation(world),
            worldReady = !isSchematicMode
        )
        KAngelDungeon.dungeonInstances[uuid] = instance
        KAngelDungeon.worldInstanceIndex[worldName] = uuid
        instance.startPlansForTrigger("PREPARE")

        // 准备阶段即传送玩家进入地牢世界，使其可在准备期间自由移动和准备
        players.forEach { p ->
            p.teleport(instance.spawnLocation)
        }

        devLog("Dungeon created: $instance")

        if (isSchematicMode) {
            val schematicPath = File(getDataFolder(), "schematics/${template.schematicFile}")
            if (schematicPath.exists()) {
                pasteSchematicAsync(
                    world = world,
                    schematicFile = schematicPath,
                    template = template,
                    onSuccess = {
                        instance.worldReady = true
                        onWorldReady?.invoke(uuid)
                    },
                    onFailure = { error ->
                        severeL("ErrorSchematicPasteFailed", template.schematicFile, error)
                        cleanupFailedDungeon(uuid, instance, worldName)
                    }
                )
            } else {
                severeL("ErrorSchematicNotFound", template.schematicFile)
                cleanupFailedDungeon(uuid, instance, worldName)
            }
        } else {
            submit {
                onWorldReady?.invoke(uuid)
            }
        }
    }

    /**
     * 清理创建失败的地牢：移除实例并卸载世界
     */
    private fun cleanupFailedDungeon(uuid: UUID, instance: DungeonInstance, worldName: String) {
        try {
            instance.stopAllPlans()
        } catch (e: Exception) {
            warningL("WarningPlanExecutionFailed", "stopAllPlans", e.message ?: "Unknown")
        }
        KAngelDungeon.dungeonInstances.remove(uuid)
        KAngelDungeon.worldInstanceIndex.remove(worldName)
        instance.players.forEach { playerUUID ->
            playerPreviousLocations.remove(playerUUID)
        }
        val worldFolder = File(Bukkit.getWorldContainer(), worldName)
        try {
            Bukkit.unloadWorld(worldName, false)
        } catch (_: Exception) {}
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder)
        }
    }
}

/**
 * 监听玩家退出事件，清理位置缓存，避免内存泄漏
 */
object DungeonPlayerTracker {
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        DungeonHelper.playerPreviousLocations.remove(uuid)
    }
}