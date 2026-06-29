package io.github.zzzyyylllty.kangeldungeon.function.javascript

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData

/**
 * 方块工具 - 提供 JS 脚本中常用的方块操作
 */
object BlockUtil {

    /**
     * 设置方块
     * JS: BlockUtil.setBlock("world", x, y, z, "DIAMOND_BLOCK")
     */
    fun setBlock(worldName: String, x: Int, y: Int, z: Int, material: String) {
        val world = Bukkit.getWorld(worldName) ?: return
        val mat = Material.getMaterial(material.uppercase()) ?: return
        world.getBlockAt(x, y, z).setType(mat)
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
        world.getBlockAt(x, y, z).breakNaturally()
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
        world.getBlockAt(x, y, z).setBlockData(data)
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
}
