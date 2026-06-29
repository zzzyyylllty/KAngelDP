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
import io.github.zzzyyylllty.kangeldungeon.util.deleteDirectory
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper.getWorldName
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
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
import taboolib.module.lang.asLangText
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

            val existingWorld = Bukkit.getWorld(worldName)
            if (existingWorld != null) {
                warningL("WarningWorldAlreadyLoaded", worldName)
                Bukkit.unloadWorld(existingWorld, false)
            }

            if (!worldFolder.exists()) {
                worldFolder.mkdirs()
            }

            infoL("InfoCopyingFiles", template.name)
            // 同步复制文件（用于插件关闭等需要立即完成的场景）
            copyWorldFiles(sourceFolder, worldFolder)

            createWorldFromCopiedFiles(template, worldName)

        } catch (e: Exception) {
            severeL("ErrorWorldCreationFailed", e.message ?: "Unknown error")
            e.printStackTrace()
            null
        }
    }

    /**
     * 从已复制好的世界文件创建 World（WorldCreator.createWorld() 必须在主线程调用）
     */
    private fun createWorldFromCopiedFiles(template: DungeonTemplate, worldName: String): World? {
        return try {
            val creator = WorldCreator(worldName)
                .type(WorldType.NORMAL)
                .environment(World.Environment.NORMAL)

            val world = creator.createWorld()
            world?.apply {
                difficulty = org.bukkit.Difficulty.NORMAL
                pvp = template.pvpEnabled
                isAutoSave = false
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
     * 异步复制世界文件 + 同步创建世界，避免阻塞主线程
     */
    private fun createWorldAsyncByRegionFile(
        template: DungeonTemplate,
        dungeonUUID: UUID,
        callback: (World?) -> Unit
    ) {
        try {
            devLog("Creating dungeon world async from template: ${template.name}")

            val worldTemplate = template.worldTemplate ?: run {
                severeL("ErrorWorldTemplateNotSpecified", template.name)
                callback(null); return
            }
            val sourceFolder = resolveSourceFolder(worldTemplate, template.name) ?: run {
                severeL("ErrorTemplateNotExist", worldTemplate)
                callback(null); return
            }
            val worldName = getWorldName(template.name, dungeonUUID)
            val worldContainer = Bukkit.getWorldContainer()
            val worldFolder = File(worldContainer, worldName)

            val existingWorld = Bukkit.getWorld(worldName)
            if (existingWorld != null) {
                warningL("WarningWorldAlreadyLoaded", worldName)
                Bukkit.unloadWorld(existingWorld, false)
            }
            if (!worldFolder.exists()) worldFolder.mkdirs()

            infoL("InfoCopyingFiles", template.name)

            // 异步复制 → 主线程创建世界
            submitAsync {
                copyWorldFiles(sourceFolder, worldFolder)
                submit {
                    val world = createWorldFromCopiedFiles(template, worldName)
                    callback(world)
                }
            }
        } catch (e: Exception) {
            severeL("ErrorWorldCreationFailed", e.message ?: "Unknown error")
            e.printStackTrace()
            callback(null)
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

            // 还原玩家放置的方块（clearOnEnd）
            val placedBlocks = KAngelDungeon.playerPlacedBlocks.remove(worldName)
            if (placedBlocks != null) {
                val template = dungeonInstance.getTemplate()
                if (template?.playerBlocks?.clearOnEnd == true) {
                    for ((key, blockDataStr) in placedBlocks) {
                        val pos = parseBlockPos(key) ?: continue
                        try {
                            val blockData = Bukkit.createBlockData(blockDataStr)
                            world.setBlockData(pos.first, pos.second, pos.third, blockData)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 卸载世界
            Bukkit.unloadWorld(world, false)
        }

        // 清理所有地牢玩家的位置缓存（包括已离线的）
        dungeonInstance.players.forEach { uuid ->
            playerPreviousLocations.remove(uuid)
        }

        // 清理怪物追踪数据
        MonsterManager.clearWorld(worldName)

        // 清理玩家放置方块计数
        KAngelDungeon.playerPlacedBlockCount.keys.removeAll { it.startsWith("$worldName:") }

        // 清理任务追踪数据
        TaskManager.clearInstance(dungeonInstance)

        // 取消所有待处理的自动复活定时器
        io.github.zzzyyylllty.kangeldungeon.event.DungeonDeathHandler.cancelAllRespawnTasks(dungeonInstance)

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
                dungeonInstance.players.forEach { KAngelDungeon.playerToInstanceIndex.remove(it, dungeonInstance.uuid) }
                KAngelDungeon.dungeonInstances.remove(dungeonInstance.uuid)
                KAngelDungeon.worldInstanceIndex.remove(worldName)
            }
        }

        if (syncDelete) {
            deleteTask()
        } else {
            // 立即同步清理地图索引，防止新实例复用冲突
            dungeonInstance.players.forEach { KAngelDungeon.playerToInstanceIndex.remove(it, dungeonInstance.uuid) }
            KAngelDungeon.dungeonInstances.remove(dungeonInstance.uuid)
            KAngelDungeon.worldInstanceIndex.remove(worldName)

            // 仅异步删除世界文件夹（纯文件 I/O，无 map 操作）
            submitAsync {
                try {
                    val worldFolder = File(Bukkit.getWorldContainer(), worldName)
                    if (worldFolder.exists()) {
                        deleteDirectory(worldFolder)
                        infoL("SuccessWorldUnloaded", worldName)
                    }
                } catch (e: Exception) {
                    severeL("ErrorDeletingWorld", e.message ?: "Unknown error")
                    e.printStackTrace()
                }
            }
        }
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
        difficultyId: String? = null,
        onWorldReady: ((UUID) -> Unit)? = null
    ): UUID? {
        val uuid = UUID.randomUUID()
        val template = KAngelDungeon.dungeonTemplates[templateName] ?: run {
            severeL("ErrorTemplateNotExist", templateName)
            return null
        }
        devLog("Create dungeon $templateName" + if (difficultyId != null) " (difficulty: $difficultyId)" else "")

        // 检查加入要求（join-requirements）
        val joinReq = template.joinRequirements
        if (joinReq.minLevel > 0 && leader.level < joinReq.minLevel) {
            leader.sendMessage(io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("WarningJoinRequirementLevel", joinReq.minLevel.toString()))
            return null
        }
        for (perm in joinReq.requiredPermissions) {
            if (!leader.hasPermission(perm)) {
                leader.sendMessage(io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("WarningJoinRequirementPermission"))
                return null
            }
        }
        if (joinReq.requiredMoney > 0) {
            // 检测 Vault 经济插件（通过反射避免编译时依赖）
            val econBalance: Double? = try {
                val servicesManager = Bukkit.getServer().javaClass.getMethod("getServicesManager").invoke(Bukkit.getServer())
                val econClass = Class.forName("net.milkbowl.vault.economy.Economy")
                val loadMethod = servicesManager.javaClass.getMethod("load", Class::class.java)
                val econ = loadMethod.invoke(servicesManager, econClass)
                econ?.javaClass?.getMethod("getBalance", org.bukkit.OfflinePlayer::class.java)?.invoke(econ, leader) as? Double
            } catch (_: Exception) { null }
            if (econBalance != null && econBalance < joinReq.requiredMoney) {
                leader.sendMessage(io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("WarningJoinRequirementMoney", joinReq.requiredMoney.toString()))
                return null
            }
        }
        for (reqItem in joinReq.requiredItems) {
            val mat = try { org.bukkit.Material.valueOf(reqItem.material.uppercase()) } catch (_: Exception) { continue }
            val count = leader.inventory.all(mat).values.sumOf { it.amount }
            if (count < reqItem.amount) {
                leader.sendMessage(io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("WarningJoinRequirementItem", reqItem.material, reqItem.amount.toString()))
                return null
            }
        }

        // 缓存所有玩家进入地牢前的位置
        players.forEach { player ->
            playerPreviousLocations[player.uniqueId] = player.location.clone()
        }

        // 共享回调：世界创建成功后完成实例初始化
        val onWorldCreated: (World?) -> Unit = { world ->
            if (world != null) {
                completeDungeonCreation(template, uuid, world, players, leader, meta, onWorldReady, difficultyId)
            } else {
                devLog("Dungeon world creation failed, cleaning up")
                players.forEach { playerPreviousLocations.remove(it.uniqueId) }
            }
        }

        if (template.worldTemplate != null) {
            // Region 模式：异步复制文件避免阻塞主线程，完成后在主线程创建世界
            createWorldAsyncByRegionFile(template, uuid, onWorldCreated)
        } else {
            // Schematic 模式：创建扁平世界很快，提交到主线程执行
            submit {
                val world = createDungeonWorld(template, uuid)
                onWorldCreated(world)
            }
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
        onWorldReady: ((UUID) -> Unit)?,
        difficultyId: String? = null
    ) {
        val worldName = DungeonHelper.getWorldName(template.name, uuid)
        val finalMeta = meta.toMutableMap()
        finalMeta["name"] = template.name
        finalMeta["template"] = template

        // 合并难度起始 meta
        if (difficultyId != null) {
            val diffMeta = io.github.zzzyyylllty.kangeldungeon.data.load.getDifficultyGlobalMeta(template.name, difficultyId)
            diffMeta.filterValues { it != null }.forEach { (k, v) -> finalMeta[k] = v!! }
            finalMeta["difficulty"] = difficultyId
        }

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
            worldReady = !isSchematicMode,
            difficultyId = difficultyId
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
     * 清理创建失败的地牢：委托给 unloadDungeonWorld 统一处理
     */
    private fun cleanupFailedDungeon(uuid: UUID, instance: DungeonInstance, worldName: String) {
        unloadDungeonWorld(instance, syncDelete = true)
    }
}

/**
 * 将 "x,y,z" 格式的坐标字符串解析为 Triple
 */
private fun parseBlockPos(key: String): Triple<Int, Int, Int>? {
    val parts = key.split(",")
    if (parts.size != 3) return null
    return try {
        Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    } catch (_: NumberFormatException) {
        null
    }
}

/**
 * 监听到玩家退出事件，清理位置缓存和地牢索引，避免内存泄漏
 */
object DungeonPlayerTracker {
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        DungeonHelper.playerPreviousLocations.remove(uuid)

        // 清理 playerToInstanceIndex，允许玩家重连后加入新地牢
        val instanceUuid = KAngelDungeon.playerToInstanceIndex.remove(uuid)
        if (instanceUuid != null) {
            val instance = KAngelDungeon.dungeonInstances[instanceUuid]
            if (instance != null) {
                // 记录掉线时间供重连机制使用
                instance.playerDisconnectTimes[uuid] = System.currentTimeMillis()
                // 将离线玩家标记为死亡，以便地牢不会因掉线玩家而卡住
                instance.markPlayerDead(event.player)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 已在其他地牢中（如刚离开一个地牢又进入），不需要重连
        if (KAngelDungeon.playerToInstanceIndex.containsKey(uuid)) return

        // 查找玩家是否在某个活跃地牢的死亡名单中
        for (instance in KAngelDungeon.dungeonInstances.values) {
            if (uuid !in instance.players || uuid !in instance.deadPlayers) continue

            val disconnectTime = instance.playerDisconnectTimes[uuid] ?: continue
            val template = instance.getTemplate() ?: continue
            val timeout = template.miscConfig.reconnectTimeout
            if (timeout <= 0) {
                instance.playerDisconnectTimes.remove(uuid)
                break  // 0=禁用重连
            }

            val elapsed = (System.currentTimeMillis() - disconnectTime) / 1000
            if (elapsed <= timeout) {
                // 重连成功：恢复索引、复活、传送
                KAngelDungeon.playerToInstanceIndex[uuid] = instance.uuid
                instance.playerDisconnectTimes.remove(uuid)
                instance.deadPlayers.remove(uuid)
                player.teleport(instance.spawnLocation)
                player.health = player.maxHealth
                devLog("Player ${player.name} reconnected to dungeon ${instance.templateName}")
            } else {
                // 超时，移除掉线记录（玩家不再能自动重连）
                instance.playerDisconnectTimes.remove(uuid)
                devLog("Player ${player.name} reconnect timeout for dungeon ${instance.templateName}")
            }
            break  // 一个玩家只能在一个地牢中
        }
    }
}