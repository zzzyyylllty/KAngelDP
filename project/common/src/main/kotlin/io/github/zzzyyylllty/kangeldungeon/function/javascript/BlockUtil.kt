package io.github.zzzyyylllty.kangeldungeon.function.javascript

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import taboolib.common.platform.function.submit

/**
 * 方块工具 - 提供 JS 脚本中常用的方块操作
 */
object BlockUtil {

    /**
     * 确保在主线程执行 Bukkit API 调用
     */
    private fun runOnMain(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            action()
        } else {
            submit { action() }
        }
    }

    /**
     * 设置方块
     * JS: BlockUtil.setBlock("world", x, y, z, "DIAMOND_BLOCK")
     */
    fun setBlock(worldName: String, x: Int, y: Int, z: Int, material: String) {
        val world = Bukkit.getWorld(worldName) ?: return
        val mat = Material.getMaterial(material.uppercase()) ?: return
        runOnMain { world.getBlockAt(x, y, z).setType(mat) }
    }

    /**
     * 获取方块类型名
     * JS: BlockUtil.getBlockType("world", x, y, z)
     */
    fun getBlockType(worldName: String, x: Int, y: Int, z: Int): String {
        val world = Bukkit.getWorld(worldName) ?: return "UNKNOWN"
        return world.getBlockAt(x, y, z).type.name
    }

    /**
     * 判断方块是否为实心
     * JS: BlockUtil.isSolid("world", x, y, z)
     */
    fun isSolid(worldName: String, x: Int, y: Int, z: Int): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return false
        return world.getBlockAt(x, y, z).type.isSolid
    }

    /**
     * 判断方块是否为空气
     * JS: BlockUtil.isAir("world", x, y, z)
     */
    fun isAir(worldName: String, x: Int, y: Int, z: Int): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return false
        return world.getBlockAt(x, y, z).type.isAir
    }

    /**
     * 破坏方块（模拟自然破坏，掉落物品）
     * JS: BlockUtil.breakBlock("world", x, y, z)
     */
    fun breakBlock(worldName: String, x: Int, y: Int, z: Int) {
        val world = Bukkit.getWorld(worldName) ?: return
        runOnMain { world.getBlockAt(x, y, z).breakNaturally() }
    }

    /**
     * 获取方块数据
     * JS: BlockUtil.getBlockData("world", x, y, z)
     */
    fun getBlockData(worldName: String, x: Int, y: Int, z: Int): BlockData? {
        val world = Bukkit.getWorld(worldName) ?: return null
        return world.getBlockAt(x, y, z).blockData
    }

    /**
     * 设置方块数据
     * JS: BlockUtil.setBlockData("world", x, y, z, blockData)
     */
    fun setBlockData(worldName: String, x: Int, y: Int, z: Int, data: BlockData) {
        val world = Bukkit.getWorld(worldName) ?: return
        runOnMain { world.getBlockAt(x, y, z).setBlockData(data) }
    }

    /**
     * 获取方块硬度（-1 为基岩类不可破坏）
     * JS: BlockUtil.getHardness("world", x, y, z)
     */
    fun getHardness(worldName: String, x: Int, y: Int, z: Int): Float {
        val world = Bukkit.getWorld(worldName) ?: return 0f
        return world.getBlockAt(x, y, z).type.hardness
    }

    /**
     * 获取方块光照等级
     * JS: BlockUtil.getLightLevel("world", x, y, z)
     */
    fun getLightLevel(worldName: String, x: Int, y: Int, z: Int): Int {
        val world = Bukkit.getWorld(worldName) ?: return 0
        return world.getBlockAt(x, y, z).lightLevel.toInt()
    }

    /**
     * 判断方块是否为可替代的（如草、水）
     * JS: BlockUtil.isReplaceable("world", x, y, z)
     */
    fun isReplaceable(worldName: String, x: Int, y: Int, z: Int): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return false
        return world.getBlockAt(x, y, z).isReplaceable
    }

    /**
     * 判断方块是否为可通行（如门、活板门）
     * JS: BlockUtil.isPassable("world", x, y, z)
     */
    fun isPassable(worldName: String, x: Int, y: Int, z: Int): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return false
        return world.getBlockAt(x, y, z).isPassable
    }

    /**
     * 获取相邻方块的相对方向
     * JS: BlockUtil.getFace(block, neighbor)  — 返回方向名如 "NORTH", "UP"
     */
    fun getFace(fromWorld: String, fromX: Int, fromY: Int, fromZ: Int, toWorld: String, toX: Int, toY: Int, toZ: Int): String? {
        val fromW = Bukkit.getWorld(fromWorld) ?: return null
        val toW = Bukkit.getWorld(toWorld) ?: return null
        if (fromW !== toW) return null
        val fromBlock = fromW.getBlockAt(fromX, fromY, fromZ)
        val toBlock = toW.getBlockAt(toX, toY, toZ)
        val face = fromBlock.getFace(toBlock) ?: return null
        return face.name
    }

    // ==================== 批量操作 ====================

    /**
     * 批量填充方块（填充长方体区域）
     * JS: BlockUtil.setBlocks("world", x1, y1, z1, x2, y2, z2, "STONE")
     */
    fun setBlocks(worldName: String, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, material: String) {
        val world = Bukkit.getWorld(worldName) ?: return
        val mat = Material.getMaterial(material.uppercase()) ?: return
        runOnMain {
            val minX = minOf(x1, x2); val maxX = maxOf(x1, x2)
            val minY = minOf(y1, y2); val maxY = maxOf(y1, y2)
            val minZ = minOf(z1, z2); val maxZ = maxOf(z1, z2)
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        world.getBlockAt(x, y, z).setType(mat)
                    }
                }
            }
        }
    }

    // ==================== 生态群系 ====================

    /**
     * 获取生态群系（Biome）名称
     * JS: BlockUtil.getBiome("world", x, y, z)
     */
    fun getBiome(worldName: String, x: Int, y: Int, z: Int): String {
        val world = Bukkit.getWorld(worldName) ?: return "UNKNOWN"
        return world.getBiome(x, y, z).name()
    }

    // ==================== 液体检测 ====================

    /**
     * 判断方块是否为液体（水、岩浆等）
     * JS: BlockUtil.isLiquid("world", x, y, z)
     */
    fun isLiquid(worldName: String, x: Int, y: Int, z: Int): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return false
        val type = world.getBlockAt(x, y, z).type
        return type == Material.WATER || type == Material.LAVA || type == Material.BUBBLE_COLUMN
    }

    // ==================== 位置工具 ====================

    /**
     * 获取方块的 Location 对象
     * JS: BlockUtil.getLocation("world", x, y, z)
     */
    fun getLocation(worldName: String, x: Int, y: Int, z: Int): org.bukkit.Location? {
        val world = Bukkit.getWorld(worldName) ?: return null
        return org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
}
