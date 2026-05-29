package io.github.zzzyyylllty.kangeldungeon.function.kether.script

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import org.bukkit.entity.Player
import taboolib.module.kether.*

/**
 * KAngelDungeon Kether 脚本动作
 *
 * 查询:
 *   kangeldp inst                     -> 获取玩家所在地牢实例
 *   kangeldp inside [named <dungeon>] -> 玩家是否在(某)地牢中
 *   kangeldp state                    -> 地牢状态
 *   kangeldp template                 -> 地牢模板名
 *   kangeldp uuid                     -> 地牢实例 UUID
 *
 * 流程控制:
 *   kangeldp complete                 -> 通关地牢
 *   kangeldp fail                     -> 失败地牢
 *   kangeldp eval <js>                -> 在地牢实例上执行 JS
 *   kangeldp script <name>            -> 执行已命名的地牢脚本
 *
 * 元数据:
 *   kangeldp meta get <key>           -> 获取元数据值
 *   kangeldp meta set <key> <value>   -> 设置元数据
 *   kangeldp meta add <key> <value>   -> 增加元数据(数字)
 *
 * 障碍物:
 *   kangeldp obstacle open <id>       -> 打开障碍物
 *   kangeldp obstacle openforce <id>  -> 强制打开障碍物
 *   kangeldp obstacle prepare <id>    -> 准备障碍物
 *   kangeldp obstacle activate <id>   -> 激活障碍物
 *
 * 怪物:
 *   kangeldp monster spawn <id>       -> 生成怪物组
 *   kangeldp monster clear            -> 清除所有怪物
 *   kangeldp monster range <id> <min> <max> -> 设置激活距离
 *   kangeldp monster range_reset <id> -> 重置激活距离为默认
 *
 * 消息:
 *   kangeldp tellall <msg>            -> 发送消息给所有玩家
 *   kangeldp titleall <title> [subtitle] -> 发送标题
 *   kangeldp actionbarall <msg>       -> 发送动作栏
 *
 * 计划:
 *   kangeldp plan start <trigger>     -> 启动指定触发器计划
 *   kangeldp plan stop                -> 停止所有计划
 */
@KetherParser(["kangeldp", "kdp"], shared = true)
fun actionKAngelDungeon() = scriptParser {
    it.switch {
        // ========== 查询 ==========

        case("inst") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon() }
        }

        case("inside") {
            val dungeonName = try {
                it.mark()
                it.expect("named")
                it.nextParsedAction()
            } catch (_: Exception) {
                it.reset()
                null
            }
            if (dungeonName != null) {
                actionFuture { future ->
                    val player = script().sender?.castSafely<Player>()
                    val inst = player?.findDungeon()
                    if (inst == null) {
                        future.complete(false)
                    } else {
                        newFrame(dungeonName).run<Any>().thenAccept { name ->
                            future.complete(inst.templateName.equals(name.toString(), true))
                        }
                    }
                }
            } else {
                actionNow { (script().sender?.castSafely<Player>())?.findDungeon() != null }
            }
        }

        case("state") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon()?.state?.name ?: "NONE" }
        }

        case("template") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon()?.templateName ?: "NONE" }
        }

        case("uuid") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon()?.uuid?.toString() ?: "NONE" }
        }

        // ========== 流程控制 ==========

        case("complete") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon()?.complete() ?: false }
        }

        case("fail") {
            actionNow { (script().sender?.castSafely<Player>())?.findDungeon()?.fail() ?: false }
        }

        case("eval") {
            val js = it.nextParsedAction()
            actionFuture { future ->
                val player = script().sender?.castSafely<Player>()
                val inst = player?.findDungeon()
                if (inst == null) {
                    future.complete(null)
                } else {
                    newFrame(js).run<Any>().thenAccept { code ->
                        try {
                            val data = defaultData + mapOf("instance" to inst, "template" to inst.getTemplate())
                            GraalJsUtil.cachedEval(code.toString(), data)
                        } catch (_: Exception) { }
                        future.complete(null)
                    }
                }
            }
        }

        case("script") {
            val name = it.nextParsedAction()
            actionFuture { future ->
                val player = script().sender?.castSafely<Player>()
                newFrame(name).run<Any>().thenAccept { n ->
                    val instance = player?.findDungeon()
                    val scripts = instance?.let { KAngelDungeon.dungeonScripts[it.templateName] }
                    val s = scripts?.get(n.toString())
                    if (s != null) {
                        try {
                            val data = defaultData + mapOf("instance" to instance, "template" to instance?.getTemplate())
                            s.onRun?.let { GraalJsUtil.cachedEval(it, data) }
                            s.onPost?.let { GraalJsUtil.cachedEval(it, data) }
                        } catch (_: Exception) { }
                    }
                    future.complete(null)
                }
            }
        }

        // ========== 元数据 ==========

        case("meta") {
            val metaCmd = it.nextToken()
            when (metaCmd.lowercase()) {
                "get" -> {
                    val key = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(key).run<Any>().thenAccept { k ->
                                future.complete(inst.getMeta(k.toString()) ?: "NONE")
                            }
                        }
                    }
                }
                "set" -> {
                    val key = it.nextParsedAction()
                    val value = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(key).run<Any>().thenAccept { k ->
                                newFrame(value).run<Any>().thenAccept { v ->
                                    inst.setMeta(k.toString(), v.toString())
                                    future.complete(null)
                                }
                            }
                        }
                    }
                }
                "add" -> {
                    val key = it.nextParsedAction()
                    val value = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(key).run<Any>().thenAccept { k ->
                                newFrame(value).run<Any>().thenAccept { v ->
                                    inst.addMeta(k.toString(), v.toString().toDoubleOrNull() ?: 1.0)
                                    future.complete(null)
                                }
                            }
                        }
                    }
                }
                else -> error("Unknown meta command: $metaCmd")
            }
        }

        // ========== 障碍物 ==========

        case("obstacle") {
            val obsCmd = it.nextToken()
            val id = it.nextParsedAction()
            actionFuture { future ->
                val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                if (inst == null) {
                    future.complete(false)
                } else {
                    newFrame(id).run<Any>().thenAccept { idVal ->
                        val cfg = KAngelDungeon.dungeonObstacleConfigs[inst.templateName]?.get(idVal.toString())
                            ?: KAngelDungeon.obstacleConfigs[idVal.toString()]
                        future.complete(when (obsCmd.lowercase()) {
                            "open" -> cfg?.let { ObstacleManager.openObstacle(inst, it) } ?: false
                            "openforce" -> cfg?.let { ObstacleManager.openObstacleForce(inst, it) } ?: false
                            "prepare" -> cfg?.let { ObstacleManager.prepareObstacle(inst, it) } ?: false
                            "activate" -> cfg?.let { ObstacleManager.activateObstacle(inst, it) } ?: false
                            else -> false
                        })
                    }
                }
            }
        }

        // ========== 怪物 ==========

        case("monster") {
            val monCmd = it.nextToken()
            when (monCmd.lowercase()) {
                "spawn" -> {
                    val id = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(false)
                        } else {
                            newFrame(id).run<Any>().thenAccept { idVal ->
                                future.complete(inst.spawnMonsters(idVal.toString()))
                            }
                        }
                    }
                }
                "range" -> {
                    val id = it.nextParsedAction()
                    val min = it.nextParsedAction()
                    val max = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(id).run<Any>().thenCompose { idVal ->
                                newFrame(min).run<Any>().thenCompose { minVal ->
                                    newFrame(max).run<Any>().thenAccept { maxVal ->
                                        inst.setMonsterActivationRangeMin(idVal.toString(), minVal.toString().toDoubleOrNull())
                                        inst.setMonsterActivationRangeMax(idVal.toString(), maxVal.toString().toDoubleOrNull())
                                        future.complete(null)
                                    }
                                }
                            }
                        }
                    }
                }
                "range_reset" -> {
                    val id = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(id).run<Any>().thenAccept { idVal ->
                                inst.resetMonsterActivationRange(idVal.toString())
                                future.complete(null)
                            }
                        }
                    }
                }
                "clear" -> {
                    actionNow {
                        (script().sender?.castSafely<Player>())?.findDungeon()?.clearAllMobs(); null
                    }
                }
                else -> error("Unknown monster command: $monCmd")
            }
        }

        // ========== 消息 ==========

        case("tellall") {
            val msg = it.nextParsedAction()
            actionFuture { future ->
                val player = script().sender?.castSafely<Player>()
                newFrame(msg).run<Any>().thenAccept { text ->
                    player?.findDungeon()?.sendMessageToAllPlayers(text.toString())
                    future.complete(null)
                }
            }
        }

        case("titleall") {
            val title = it.nextParsedAction()
            val subtitle = try {
                it.mark()
                it.nextParsedAction()
            } catch (_: Exception) {
                it.reset()
                null
            }
            actionFuture { future ->
                val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                if (inst == null) {
                    future.complete(null)
                } else {
                    newFrame(title).run<Any>().thenAccept { t ->
                        if (subtitle != null) {
                            newFrame(subtitle).run<Any>().thenAccept { s ->
                                inst.sendTitleToAllPlayers(t.toString(), s.toString())
                                future.complete(null)
                            }
                        } else {
                            inst.sendTitleToAllPlayers(t.toString())
                            future.complete(null)
                        }
                    }
                }
            }
        }

        case("actionbarall") {
            val msg = it.nextParsedAction()
            actionFuture { future ->
                val player = script().sender?.castSafely<Player>()
                newFrame(msg).run<Any>().thenAccept { text ->
                    player?.findDungeon()?.sendActionBarToAllPlayers(text.toString())
                    future.complete(null)
                }
            }
        }

        // ========== 计划 ==========

        case("plan") {
            val planCmd = it.nextToken()
            when (planCmd.lowercase()) {
                "start" -> {
                    val trigger = it.nextParsedAction()
                    actionFuture { future ->
                        val inst = (script().sender?.castSafely<Player>())?.findDungeon()
                        if (inst == null) {
                            future.complete(null)
                        } else {
                            newFrame(trigger).run<Any>().thenAccept { t ->
                                inst.startPlansForTrigger(t.toString())
                                future.complete(null)
                            }
                        }
                    }
                }
                "stop" -> {
                    actionNow {
                        (script().sender?.castSafely<Player>())?.findDungeon()?.stopAllPlans(); null
                    }
                }
                else -> error("Unknown plan command: $planCmd")
            }
        }
    }
}

private fun Player.findDungeon(): DungeonInstance? {
    val instanceUuid = KAngelDungeon.playerToInstanceIndex[uniqueId] ?: return null
    return KAngelDungeon.dungeonInstances[instanceUuid]
}
