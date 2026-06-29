package io.github.zzzyyylllty.kangeldungeon.function.javascript

import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmUtil
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player

/**
 * 实体工具 - 提供 JS 脚本中常用的实体操作
 */
object EntityUtil {

    /**
     * 判断实体是否存活
     * JS: EntityUtil.isAlive(entity)
     */
    fun isAlive(entity: LivingEntity): Boolean {
        return !entity.isDead && entity.health > 0
    }

    /**
     * 对实体造成伤害
     * JS: EntityUtil.damage(entity, 10.0)
     */
    fun damage(entity: LivingEntity, amount: Double) {
        entity.damage(amount.coerceAtLeast(0.0))
    }

    /**
     * 移除实体
     * JS: EntityUtil.remove(entity)
     */
    fun remove(entity: Entity) {
        entity.remove()
    }

    /**
     * 获取位置附近的实体
     * JS: EntityUtil.getEntitiesNear(worldName, x, y, z, radius)
     */
    fun getEntitiesNear(worldName: String, x: Double, y: Double, z: Double, radius: Double): List<Entity> {
        val world = Bukkit.getWorld(worldName) ?: return emptyList()
        val loc = Location(world, x, y, z)
        return loc.getNearbyEntities(radius, radius, radius).toList()
    }

    /**
     * 获取位置附近的玩家
     * JS: EntityUtil.getPlayersNear(worldName, x, y, z, radius)
     */
    fun getPlayersNear(worldName: String, x: Double, y: Double, z: Double, radius: Double): List<Player> {
        val world = Bukkit.getWorld(worldName) ?: return emptyList()
        val loc = Location(world, x, y, z)
        return loc.getNearbyEntities(radius, radius, radius).filterIsInstance<Player>()
    }

    /**
     * 获取位置附近的生物
     * JS: EntityUtil.getLivingEntitiesNear(worldName, x, y, z, radius)
     */
    fun getLivingEntitiesNear(worldName: String, x: Double, y: Double, z: Double, radius: Double): List<LivingEntity> {
        val world = Bukkit.getWorld(worldName) ?: return emptyList()
        val loc = Location(world, x, y, z)
        return loc.getNearbyEntities(radius, radius, radius).filterIsInstance<LivingEntity>()
    }

    /**
     * 设置实体发光效果
     * JS: EntityUtil.setGlowing(entity, true)
     */
    fun setGlowing(entity: Entity, glowing: Boolean) {
        entity.isGlowing = glowing
    }

    /**
     * 设置实体自定义名称
     * JS: EntityUtil.setCustomName(entity, "Boss")
     */
    fun setCustomName(entity: Entity, name: String) {
        entity.customName(mmUtil.deserialize(name))
    }

    /**
     * 获取实体自定义名称
     * JS: EntityUtil.getCustomName(entity)
     */
    fun getCustomName(entity: Entity): String? {
        return entity.customName()?.let { mmUtil.serialize(it) }
    }

    /**
     * 判断实体是否为敌对怪物
     * JS: EntityUtil.isMonster(entity)
     */
    fun isMonster(entity: Entity): Boolean {
        return entity is Monster
    }

    /**
     * 设置实体的是否可被拾起（物品）
     * JS: EntityUtil.setPickupDelay(entity, ticks)
     */
    fun setPickupDelay(entity: Entity, ticks: Int) {
        if (entity is org.bukkit.entity.Item) entity.pickupDelay = ticks
    }

    /**
     * 设置实体无敌时间（tick）
     * JS: EntityUtil.setInvulnerable(entity, true)
     */
    fun setInvulnerable(entity: Entity, invulnerable: Boolean) {
        entity.isInvulnerable = invulnerable
    }

    /**
     * 设置实体是否沉默（禁用 AI 声音）
     * JS: EntityUtil.setSilent(entity, true)
     */
    fun setSilent(entity: Entity, silent: Boolean) {
        entity.isSilent = silent
    }

    /**
     * 让实体着火
     * JS: EntityUtil.setFireTicks(entity, 100)
     */
    fun setFireTicks(entity: Entity, ticks: Int) {
        entity.fireTicks = ticks.coerceAtLeast(0)
    }

    /**
     * 获取实体类型名
     * JS: EntityUtil.getType(entity)
     */
    fun getType(entity: Entity): String {
        return entity.type.name
    }

    /**
     * 传送实体到坐标
     * JS: EntityUtil.teleport(entity, x, y, z, worldName?)
     */
    fun teleport(entity: Entity, x: Double, y: Double, z: Double, worldName: String? = null) {
        val world = if (worldName != null) Bukkit.getWorld(worldName) else entity.world
        if (world != null) entity.teleport(Location(world, x, y, z))
    }

    /**
     * 获取实体的 UUID 字符串
     * JS: EntityUtil.getUniqueId(entity)
     */
    fun getUniqueId(entity: Entity): String {
        return entity.uniqueId.toString()
    }
}
