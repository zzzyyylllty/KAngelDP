package io.github.zzzyyylllty.kangeldungeon.util.monster

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.event.DungeonMobKillEvent
import io.github.zzzyyylllty.kangeldungeon.event.MonsterGroupClearEvent
import io.github.zzzyyylllty.kangeldungeon.event.MonsterSpawnPostEvent
import io.github.zzzyyylllty.kangeldungeon.event.MonsterSpawnPreEvent
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import ink.ptms.um.Mythic
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRemoveEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 怪物管理器 - 管理地牢中的怪物生成与追踪
 */
object MonsterManager {

    // 活跃的怪物组实例: dungeonWorldName -> (configId -> MonsterInstance)
    val activeMonsters = ConcurrentHashMap<String, ConcurrentHashMap<String, MonsterInstance>>()
    // 实体UUID到所属世界的反向映射，用于O(1)查询击杀归属
    private val entityOwnerMap = ConcurrentHashMap<UUID, String>()
    // 实体UUID到configId的反向索引，用于O(1)击杀查找组
    private val entityToConfigMap = ConcurrentHashMap<UUID, String>()

    // 未完成的交错生成任务: worldName -> List<task>，用于世界卸载时取消
    private val pendingStaggeredTasks = ConcurrentHashMap<String, MutableList<PlatformExecutor.PlatformTask>>()

    /**
     * 生成配置中所有的怪物
     * @return 生成的实体列表，失败返回 null（条件不满足时也返回 null）
     */
    fun spawnMonsters(instance: DungeonInstance, config: MonsterConfig): List<LivingEntity>? {
        val world = instance.world ?: return null

        // 不活跃的组跳过生成
        if (!config.active) return null

        // 检查 spawnDelay（地牢开始后的延迟 ticks）
        if (config.spawnDelay > 0) {
            val startTime = instance.startedAt ?: instance.createdAt
            val elapsedTicks = (System.currentTimeMillis() - startTime) / 50L
            if (elapsedTicks < config.spawnDelay) return null
        }

        // 检查 spawnCondition（JS 条件表达式）
        if (!config.spawnCondition.isNullOrBlank()) {
            try {
                val data = defaultData + mapOf(
                    "instance" to instance,
                    "template" to instance.getTemplate(),
                    "monsterConfig" to config
                )
                val result = GraalJsUtil.cachedEval(config.spawnCondition, data)
                val ok = when (result) {
                    is Boolean -> result
                    is Number -> result.toDouble() != 0.0
                    else -> result != null
                }
                if (!ok) return null
            } catch (e: Exception) {
                warningL("WarningMonsterSpawnConditionFailed", config.id, e.message ?: "Unknown error")
                return null
            }
        }

        val event = MonsterSpawnPreEvent(instance, config)
        event.call()
        if (event.isCancelled) return null

        try {
            val spawned = mutableListOf<LivingEntity>()

            for (entry in config.monsters) {
                val baseLoc = Location(world, entry.location.x, entry.location.y, entry.location.z)
                if (config.spawnInterval > 0 && entry.amount > 1) {
                    val mobs = spawnMobGroupStaggered(baseLoc, entry, config.spawnInterval, config.id, instance)
                    spawned.addAll(mobs)
                } else {
                    val mobs = spawnMobGroup(baseLoc, entry)
                    spawned.addAll(mobs)
                }
            }

            // 注册追踪（实体UUID→世界名反向映射，用于O(1)击杀查询）
            val worldKey = instance.worldName
            val entityIds = spawned.map { it.uniqueId }

            // 应用 config 级别的 healthMultiplier / damageMultiplier
            if (config.healthMultiplier != 1.0 || config.damageMultiplier != 1.0) {
                spawned.forEach { applyConfigModifiers(it, config) }
            }
            // 应用难度动态缩放（按玩家数调整怪物属性）
            val scaling = instance.getTemplate()?.difficultyScaling
            if (scaling != null && scaling.enabled) {
                spawned.forEach { applyDynamicScaling(it, instance) }
            }

            entityIds.forEach {
                entityOwnerMap[it] = worldKey
                entityToConfigMap[it] = config.id
            }
            // 更新 world→instance 反向索引，写入前确认实例仍在活跃列表中（防止竞态：清理与生成并发）
            if (KAngelDungeon.dungeonInstances.containsKey(instance.uuid)) {
                KAngelDungeon.worldInstanceIndex[worldKey] = instance.uuid
            }

            val groupMap = activeMonsters.getOrPut(worldKey) { ConcurrentHashMap() }
            val mobInstance = MonsterInstance(
                config = config,
                dungeonInstance = instance,
                spawnedMobs = ConcurrentHashMap.newKeySet<UUID>().also { it.addAll(entityIds) }
            )
            groupMap[config.id] = mobInstance

            // 执行 onSpawn 代理脚本
            runMonsterAgent(config.agent?.onSpawn, instance, config, mapOf("spawnedMobs" to spawned))

            MonsterSpawnPostEvent(instance, config, spawned).call()
            instance.meta.add("monster.spawn", 1)
            instance.meta.add("monster.spawn.${config.id}", 1)
            TaskManager.triggerTasks(instance, "MONSTER_SPAWN", mapOf(
                "configId" to config.id,
                "spawnedMobs" to spawned
            ))
            return spawned
        } catch (e: Exception) {
            warningL("WarningMonsterSpawnFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
            return null
        }
    }

    /**
     * 生成单个怪物组
     */
    private fun spawnMobGroup(baseLoc: Location, entry: MonsterSpawnEntry): List<LivingEntity> {
        val mobs = mutableListOf<LivingEntity>()
        val world = baseLoc.world ?: return mobs

        for (i in 0 until entry.amount) {
            val loc = if (entry.scattered > 0) {
                val offsetX = (Math.random() * 2 - 1) * entry.scattered
                val offsetZ = (Math.random() * 2 - 1) * entry.scattered
                baseLoc.clone().add(offsetX, 0.0, offsetZ)
            } else {
                baseLoc
            }

            val entity = spawnMob(world, loc, entry.mob, entry.level)
            if (entity != null) {
                mobs.add(entity)
            }
        }

        return mobs
    }

    /**
     * 带间隔依次生成一组怪物
     * @param baseLoc 基准位置
     * @param entry 生成条目
     * @param interval 间隔（tick）
     * @param configId 所属怪物配置ID
     * @param instance 地牢实例
     */
    private fun spawnMobGroupStaggered(baseLoc: Location, entry: MonsterSpawnEntry, interval: Long, configId: String, instance: DungeonInstance): List<LivingEntity> {
        val spawned = mutableListOf<LivingEntity>()
        val world = baseLoc.world ?: return spawned
        val worldKey = instance.worldName

        for (i in 0 until entry.amount) {
            val loc = if (entry.scattered > 0) {
                val offsetX = (Math.random() * 2 - 1) * entry.scattered
                val offsetZ = (Math.random() * 2 - 1) * entry.scattered
                baseLoc.clone().add(offsetX, 0.0, offsetZ)
            } else {
                baseLoc.clone()
            }
            if (i == 0) {
                val entity = spawnMob(world, loc, entry.mob, entry.level)
                if (entity != null) spawned.add(entity)
            } else {
                val delay = i * interval
                val task = taboolib.common.platform.function.submit(delay = delay) {
                    if (Bukkit.getWorld(worldKey) == null) return@submit
                    // 检查世界/怪物组是否已被清理（clearWorld 可能已经取消了此任务）
                    if (activeMonsters[worldKey]?.get(configId) == null) return@submit
                    val entity = spawnMob(world, loc, entry.mob, entry.level) ?: return@submit
                    entityOwnerMap[entity.uniqueId] = worldKey
                    entityToConfigMap[entity.uniqueId] = configId
                    activeMonsters[worldKey]?.get(configId)?.spawnedMobs?.add(entity.uniqueId)
                }
                pendingStaggeredTasks.getOrPut(worldKey) { mutableListOf() }.add(task)
            }
        }
        return spawned
    }

    /**
     * 生成单个生物
     * 支持 minecraft:xxx 标准生物和 mythicmobs:xxx MythicMobs
     */
    private fun spawnMob(world: org.bukkit.World, loc: Location, mobId: String, level: Int): LivingEntity? {
        return when {
            mobId.startsWith("minecraft:", ignoreCase = true) -> {
                spawnVanillaMob(world, loc, mobId.removePrefix("minecraft:"), level)
            }
            mobId.startsWith("mythicmobs:", ignoreCase = true) -> {
                spawnMythicMob(loc, mobId.removePrefix("mythicmobs:"), level)
            }
            else -> {
                // 没有前缀时，尝试作为标准生物处理
                spawnVanillaMob(world, loc, mobId, level)
            }
        }
    }

    /**
     * 生成标准 Minecraft 生物
     */
    private fun spawnVanillaMob(world: org.bukkit.World, loc: Location, mobId: String, level: Int): LivingEntity? {
        val entityType = try {
            EntityType.valueOf(mobId.uppercase().replace(" ", "_"))
        } catch (e: IllegalArgumentException) {
            warningL("WarningMonsterUnknownEntity", mobId)
            return null
        }

        if (!entityType.isSpawnable) return null

        val entity = world.spawnEntity(loc, entityType)
        if (entity is LivingEntity) {
            // 应用等级（调整属性）
            if (level > 0) {
                entity.setCustomName("Lv.$level ${entity.name}")
                val maxHealth = entity.maxHealth * (1.0 + level * 0.2)
                entity.maxHealth = maxHealth
                entity.health = maxHealth
            }
            // 应用生命倍率（与等级叠加）
            return applyModifiers(entity, null, level)
        }
        return null
    }

    /**
     * 应用 MonsterConfig 级别的 healthMultiplier / damageMultiplier
     * 在 spawnMonsters / respawnMonsterGroup 中对所有生成的实体调用
     */
    private fun applyConfigModifiers(entity: LivingEntity, config: MonsterConfig) {
        if (config.healthMultiplier != 1.0) {
            val oldHealth = entity.health
            val baseMax = entity.maxHealth
            val newMax = baseMax * config.healthMultiplier
            entity.maxHealth = newMax
            entity.health = newMax.coerceAtMost(oldHealth * config.healthMultiplier)
        }
        if (config.damageMultiplier != 1.0) {
            val attr = entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE)
            if (attr != null) {
                attr.baseValue = attr.baseValue * config.damageMultiplier
            }
        }
    }

    /**
     * 应用难度动态缩放（按 dungeon 玩家人数自动调整怪物属性）
     * 在 applyConfigModifiers 之后调用，两者叠加
     */
    fun applyDynamicScaling(entity: LivingEntity, instance: DungeonInstance) {
        val template = instance.getTemplate() ?: return
        val scaling = template.difficultyScaling
        if (!scaling.enabled) return

        val playerCount = instance.players.size
        val diff = playerCount - scaling.basePlayers

        if (diff == 0) return

        val healthMult = if (diff > 0) 1.0 + diff * scaling.healthMultiplierPerExtra
                         else 1.0 - diff.coerceAtLeast(1) * scaling.healthMultiplierPerLess  // diff is negative -> subtract
        val damageMult = if (diff > 0) 1.0 + diff * scaling.damageMultiplierPerExtra
                         else 1.0 - diff.coerceAtLeast(1) * scaling.damageMultiplierPerLess

        // 在设置 maxHealth 前缓存当前血量，避免设置 maxHealth 后被裁剪
        val oldHealth = entity.health
        val baseMax = entity.maxHealth
        val newMax = baseMax * healthMult.coerceAtLeast(0.1)
        entity.maxHealth = newMax
        entity.health = newMax.coerceAtMost(oldHealth * healthMult.coerceAtLeast(0.1))

        val attr = entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE)
        if (attr != null) {
            attr.baseValue = attr.baseValue * damageMult.coerceAtLeast(0.1)
        }
    }

    /**
     * 应用倍数修正（等级 + config 级属性）
     */
    private fun applyModifiers(entity: LivingEntity, config: MonsterConfig?, level: Int): LivingEntity {
        if (config != null) {
            applyConfigModifiers(entity, config)
        }
        return entity
    }

    /**
     * 生成 MythicMobs 生物（通过 Universal-Mythic 统一 API）
     * 支持 MM4 和 MM5，详见 um.txt
     * 注意：Mythic 为软依赖，首次访问其类时可能抛出 NoClassDefFoundError
     */
    private fun spawnMythicMob(loc: Location, mobId: String, level: Int): LivingEntity? {
        try {
            if (!Mythic.isLoaded()) {
                warningL("WarningMonsterSpawnFailed", mobId, "MythicMobs not loaded")
                return null
            }
            val mobType = Mythic.API.getMobType(mobId) ?: run {
                warningL("WarningMonsterUnknownEntity", "MythicMobs: $mobId")
                return null
            }
            val spawned = mobType.spawn(loc, level = level.toDouble())
            return spawned.entity as? LivingEntity
        } catch (e: Exception) {
            warningL("WarningMonsterMythicMobsSpawnFailed", mobId, e.message ?: "Unknown error")
        } catch (e: NoClassDefFoundError) {
            warningL("WarningMonsterSpawnFailed", mobId, "MythicMobs not installed (NoClassDefFoundError)")
        }
        return null
    }

    /**
     * 记录怪物击杀，当组内全部清除时触发 onAllKilled 代理
     * 使用 entityOwnerMap 反向映射实现 O(1) 查询，避免遍历所有世界和怪物组
     */
    fun onMobKill(entity: LivingEntity) {
        val entityId = entity.uniqueId
        val worldName = entityOwnerMap.remove(entityId) ?: return
        val configId = entityToConfigMap.remove(entityId) ?: return
        val instanceUuid = KAngelDungeon.worldInstanceIndex[worldName] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return

        // 记录击杀
        instance.incrementMobKills()
        instance.meta.add("mob.kill.${entity.type.name.lowercase()}", 1)

        // 触发 MOB_KILL 任务
        val killer = entity.killer
        TaskManager.triggerTasks(instance, "MOB_KILL", mapOf(
            "mobType" to entity.type.name,
            "mobName" to (entity.customName?.toString() ?: entity.type.name),
            "entity" to entity,
            "killer" to killer
        ))

        // 触发 DungeonMobKillEvent（供外部监听，如 Chemdah 任务系统）
        if (killer != null) {
            try {
                // 尝试获取 MM 怪物信息
                var mobId = entity.type.name
                var mobLevel = 0
                try {
                    if (Mythic.isLoaded()) {
                        val mmMob = Mythic.API.getMob(entity)
                        if (mmMob != null) {
                            mobId = mmMob.id
                            mobLevel = mmMob.level.toInt()
                        }
                    }
                } catch (_: NoClassDefFoundError) {
                    // MythicMobs 未安装
                }
                DungeonMobKillEvent(instance, killer, entity.type.name, entity.customName?.toString() ?: entity.type.name, mobId, mobLevel, entity).call()
                io.github.zzzyyylllty.kangeldungeon.util.stats.PlayerStatsManager.recordMobKill(killer.uniqueId)
            } catch (e: Exception) {
                warningL("WarningMonsterKillEventFailed", entity.type.name, e.message ?: "Unknown error")
            }
        }

        val groupMap = activeMonsters[worldName] ?: return
        val mobInstance = groupMap[configId] ?: return
        if (mobInstance.allKilled) return
        if (mobInstance.spawnedMobs.remove(entityId)) {
            // 触发 onEachKill（每击杀一个怪物触发一次）
            runMonsterAgent(mobInstance.config.agent?.onEachKill, instance, mobInstance.config, mapOf(
                "entity" to entity,
                "killer" to killer
            ))

            synchronized(mobInstance) {
                if (mobInstance.spawnedMobs.isEmpty() && !mobInstance.allKilled) {
                    mobInstance.allKilled = true
                    groupMap.remove(configId)
                    onGroupCleared(mobInstance)
                }
            }
        }
    }

    /**
     * 怪物组全部清除时回调
     */
    private fun onGroupCleared(mobInstance: MonsterInstance) {
        try {
            // 执行 onAllKilled 代理脚本
            runMonsterAgent(mobInstance.config.agent?.onAllKilled, mobInstance.dungeonInstance, mobInstance.config, mapOf("monsterInstance" to mobInstance))

            MonsterGroupClearEvent(mobInstance.dungeonInstance, mobInstance.config).call()
            mobInstance.dungeonInstance.meta.add("monster.group.clear", 1)
            mobInstance.dungeonInstance.meta.add("monster.group.clear.${mobInstance.config.id}", 1)
            TaskManager.triggerTasks(mobInstance.dungeonInstance, "MONSTER_GROUP_CLEAR", mapOf(
                "configId" to mobInstance.config.id,
                "monsterInstance" to mobInstance
            ))
        } catch (e: Exception) {
            warningL("WarningMonsterAgentFailed", mobInstance.config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 清理世界所有怪物追踪数据
     */
    fun clearWorld(worldName: String) {
        // 取消该世界所有未完成的交错生成任务
        pendingStaggeredTasks.remove(worldName)?.forEach { it.cancel() }

        val removed = activeMonsters.remove(worldName)
        // 清理该世界中所有实体的反向映射
        entityOwnerMap.entries.removeIf { it.value == worldName }
        // 清理 entityToConfigMap 中属于该世界的条目
        if (removed != null) {
            for (mobInstance in removed.values) {
                for (uuid in mobInstance.spawnedMobs) {
                    entityToConfigMap.remove(uuid)
                }
            }
        }
        // worldInstanceIndex 由 DungeonHelper.unloadDungeonWorld 统一管理
    }

    /**
     * 获取地牢的活跃怪物组
     */
    fun getMonsterInstances(instance: DungeonInstance): Map<String, MonsterInstance> {
        return activeMonsters[instance.worldName]?.toMap() ?: emptyMap()
    }

    /**
     * 移除指定实体的击杀追踪（用于非正常移除场景，如 clearAllMobs / clearHostileMobs）
     * 防止 entityOwnerMap 中积累已不存在的实体条目
     */
    fun removeEntityTracking(entityIds: Set<UUID>) {
        entityIds.forEach {
            entityOwnerMap.remove(it)
            entityToConfigMap.remove(it)
        }
    }

    /**
     * 实体自然消失/被移除时的清理（非击杀导致的移除）
     * 清理 entityOwnerMap、entityToConfigMap 和对应怪物组中的实体追踪
     */
    fun onEntityRemoved(entity: LivingEntity) {
        val entityId = entity.uniqueId
        val worldName = entityOwnerMap[entityId] ?: return
        val configId = entityToConfigMap[entityId]
        entityOwnerMap.remove(entityId)
        entityToConfigMap.remove(entityId)
        if (configId != null) {
            activeMonsters[worldName]?.get(configId)?.spawnedMobs?.remove(entityId)
        }
    }

    // ==================== 激活控制 ====================

    /**
     * 设置怪物组激活状态（JS 可调用）
     * JS: MonsterManager.setMonsterActive(instance, "zombies", false)
     */
    fun setMonsterActive(instance: DungeonInstance, configId: String, active: Boolean) {
        activeMonsters[instance.worldName]?.get(configId)?.active = active
    }

    /**
     * 设置怪物组重生冷却（tick），-1 恢复为 config 默认值
     * JS: MonsterManager.setMonsterCooldown(instance, "zombies", 200)
     */
    fun setMonsterCooldown(instance: DungeonInstance, configId: String, cooldownTicks: Long) {
        activeMonsters[instance.worldName]?.get(configId)?.respawnCooldownTicks = cooldownTicks
    }

    /**
     * 设置怪物组最小激活距离，null 恢复为 config 默认值
     * JS: MonsterManager.setMonsterActivationRangeMin(instance, "zombies", 5.0)
     */
    fun setMonsterActivationRangeMin(instance: DungeonInstance, configId: String, value: Double?) {
        activeMonsters[instance.worldName]?.get(configId)?.activationRangeMin = value
    }

    /**
     * 设置怪物组最大激活距离，null 恢复为 config 默认值
     * JS: MonsterManager.setMonsterActivationRangeMax(instance, "zombies", 30.0)
     */
    fun setMonsterActivationRangeMax(instance: DungeonInstance, configId: String, value: Double?) {
        activeMonsters[instance.worldName]?.get(configId)?.activationRangeMax = value
    }

    /**
     * 重置怪物组激活距离为 config 默认值
     * JS: MonsterManager.resetMonsterActivationRange(instance, "zombies")
     */
    fun resetMonsterActivationRange(instance: DungeonInstance, configId: String) {
        activeMonsters[instance.worldName]?.get(configId)?.let {
            it.activationRangeMin = null
            it.activationRangeMax = null
        }
    }

    /**
     * 获取怪物组的有效冷却值
     */
    private fun getEffectiveCooldown(mobInstance: MonsterInstance): Long {
        val override = mobInstance.respawnCooldownTicks
        return if (override >= 0) override else mobInstance.config.respawnCooldown
    }

    /**
     * 检查玩家是否在怪物组的激活距离内
     * 同时检查最小/最大激活距离，运行时覆盖优先
     */
    private fun isWithinActivationRange(instance: DungeonInstance, mobInstance: MonsterInstance): Boolean {
        val config = mobInstance.config
        val effectiveMin = mobInstance.activationRangeMin ?: config.activationRangeMin
        val effectiveMax = mobInstance.activationRangeMax ?: config.activationRangeMax

        // 两项均无限制时快速返回
        if (effectiveMin <= 0.0 && effectiveMax < 0.0) return true

        val world = instance.world ?: return false
        var spawnCenter: Location? = null
        for (entry in config.monsters) {
            spawnCenter = Location(world, entry.location.x, entry.location.y, entry.location.z)
            break
        }
        val center = spawnCenter ?: return true

        val nearestSq = world.players.minOfOrNull { p -> p.location.distanceSquared(center) } ?: return false

        return (effectiveMin <= 0.0 || nearestSq >= effectiveMin * effectiveMin) &&
            (effectiveMax < 0.0 || nearestSq <= effectiveMax * effectiveMax)
    }

    /** 清理周期计数器，每 100 tick 清理一次 stale 实体追踪 */
    @Volatile
    private var tickCounter = 0

    /**
     * 清理 entityOwnerMap / entityToConfigMap 中已不存在的实体条目
     * 解决 Spigot 下 EntityRemoveEvent 不触发导致的追踪泄漏
     */
    private fun cleanStaleEntityTracking() {
        val staleKeys = entityOwnerMap.keys.filter { uuid -> Bukkit.getEntity(uuid) == null }
        staleKeys.forEach {
            entityOwnerMap.remove(it)
            entityToConfigMap.remove(it)
        }
    }

    /**
     * 地牢 tick 时调用，处理怪物组激活检测与自动重生
     */
    fun tickDungeonMonsters(instance: DungeonInstance) {
        // 每 100 tick 清理一次已消失实体的追踪数据，避免 Spigot 下 EntityRemoveEvent 不触发导致的泄漏
        tickCounter++
        if (tickCounter % 100 == 0) {
            cleanStaleEntityTracking()
        }

        val worldKey = instance.worldName
        val groupMap = activeMonsters[worldKey]
        val world = instance.world ?: return

        // 自动生成尚未生成且配置了 spawnDelay / spawnCondition 的怪物组
        if (instance.state == DungeonState.ACTIVE) {
            val allConfigs = instance.getMonsterConfigs()
            // 按优先级排序（priority 高的优先生成）
            val sorted = allConfigs.entries.sortedByDescending { (_, config) -> config.priority }
            for ((configId, config) in sorted) {
                // 已生成的组跳过
                if (groupMap?.containsKey(configId) == true) continue
                if (!config.active) continue
                // 只处理配置了延迟或条件的组（其他保持手动调用行为）
                if (config.spawnDelay <= 0 && config.spawnCondition.isNullOrBlank()) continue
                // 尝试生成（条件不满足时会内部返回 null）
                spawnMonsters(instance, config)
            }
        }

        if (groupMap == null) return

        for ((configId, mobInstance) in groupMap) {
            // 不活跃的组跳过
            if (!mobInstance.active) continue

            val config = mobInstance.config

            // 激活距离检查（含最小/最大距离，运行时覆盖优先）
            if (!isWithinActivationRange(instance, mobInstance)) continue

            // 拴绳范围检查 — 将超出范围的怪物拉回最近的刷怪点
            if (config.leashRange > 0) {
                var spawnCenter: Location? = null
                for (entry in config.monsters) {
                    spawnCenter = Location(world, entry.location.x, entry.location.y, entry.location.z)
                    break
                }
                if (spawnCenter != null) {
                    val leashSq = config.leashRange * config.leashRange
                    for (uuid in mobInstance.spawnedMobs.toSet()) {
                        val entity = Bukkit.getEntity(uuid) ?: continue
                        if (entity.location.distanceSquared(spawnCenter) > leashSq) {
                            entity.teleport(spawnCenter)
                        }
                    }
                }
            }

            // 组已全灭且有冷却 → 自动重生
            if (mobInstance.allKilled) {
                val cooldownMs = getEffectiveCooldown(mobInstance) * 50
                if (cooldownMs <= 0) continue

                // 检查最大重生次数（防止已超出限制的组反复检查）
                if (config.maxRespawns >= 0 && mobInstance.respawnCount >= config.maxRespawns) {
                    groupMap.remove(configId)
                    continue
                }

                // 检查重生条件（respawnCondition），与初始 spawnCondition 独立
                if (!config.respawnCondition.isNullOrBlank()) {
                    try {
                        val data = defaultData + mapOf(
                            "instance" to instance,
                            "template" to instance.getTemplate(),
                            "monsterConfig" to config,
                            "respawnCount" to mobInstance.respawnCount
                        )
                        val result = GraalJsUtil.cachedEval(config.respawnCondition, data)
                        val ok = when (result) {
                            is Boolean -> result
                            is Number -> result.toDouble() != 0.0
                            else -> result != null
                        }
                        if (!ok) continue
                    } catch (e: Exception) {
                        continue
                    }
                }

                val now = System.currentTimeMillis()
                if (now - mobInstance.lastRespawnTime < cooldownMs) continue
                // 重生
                respawnMonsterGroup(instance, config, mobInstance)
            }
        }
    }

    /**
     * 重生怪物组
     */
    private fun respawnMonsterGroup(instance: DungeonInstance, config: MonsterConfig, mobInstance: MonsterInstance) {
        val world = instance.world ?: return
        try {
            // 检查最大重生次数
            if (config.maxRespawns >= 0 && mobInstance.respawnCount >= config.maxRespawns) {
                activeMonsters[instance.worldName]?.remove(config.id)
                return
            }

            val spawned = mutableListOf<LivingEntity>()
            for (entry in config.monsters) {
                val baseLoc = Location(world, entry.location.x, entry.location.y, entry.location.z)
                if (config.spawnInterval > 0 && entry.amount > 1) {
                    val mobs = spawnMobGroupStaggered(baseLoc, entry, config.spawnInterval, config.id, instance)
                    spawned.addAll(mobs)
                } else {
                    val mobs = spawnMobGroup(baseLoc, entry)
                    spawned.addAll(mobs)
                }
            }

            // 应用 config 级别的 healthMultiplier / damageMultiplier
            if (config.healthMultiplier != 1.0 || config.damageMultiplier != 1.0) {
                spawned.forEach { applyConfigModifiers(it, config) }
            }
            // 应用难度动态缩放（按玩家数调整怪物属性）
            val scaling = instance.getTemplate()?.difficultyScaling
            if (scaling != null && scaling.enabled) {
                spawned.forEach { applyDynamicScaling(it, instance) }
            }

            val entityIds = spawned.map { it.uniqueId }
            entityIds.forEach {
                entityOwnerMap[it] = instance.worldName
                entityToConfigMap[it] = config.id
            }

            mobInstance.spawnedMobs.clear()
            mobInstance.spawnedMobs.addAll(entityIds)
            mobInstance.allKilled = false
            mobInstance.lastRespawnTime = System.currentTimeMillis()
            mobInstance.respawnCount++

            // 执行 onRespawn 代理脚本
            runMonsterAgent(config.agent?.onRespawn, instance, config, mapOf("spawnedMobs" to spawned))

            instance.meta.add("monster.respawn", 1)
            instance.meta.add("monster.respawn.${config.id}", 1)
        } catch (e: Exception) {
            warningL("WarningMonsterSpawnFailed", config.id, "respawn: ${e.message}")
            e.printStackTrace()
        }
    }

    // ==================== 代理脚本 ====================

    private fun runMonsterAgent(script: String?, instance: DungeonInstance, config: MonsterConfig, extraVars: Map<String, Any?> = emptyMap()) {
        if (script.isNullOrBlank()) return
        try {
            val data = defaultData + mapOf(
                "instance" to instance,
                "template" to instance.getTemplate(),
                "monsterConfig" to config
            ) + extraVars
            GraalJsUtil.cachedEval(script, data)
        } catch (e: Exception) {
            warningL("WarningMonsterAgentFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }
}

object MonsterListener {
    @SubscribeEvent
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        MonsterManager.onMobKill(entity)
    }

    @SubscribeEvent
    fun onEntityRemove(event: EntityRemoveEvent) {
        val entity = event.entity
        if (entity is LivingEntity) {
            MonsterManager.onEntityRemoved(entity)
        }
    }
}
