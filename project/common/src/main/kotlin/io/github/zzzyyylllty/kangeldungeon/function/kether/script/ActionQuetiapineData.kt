package io.github.zzzyyylllty.kangeldungeon.function.kether.script

import io.github.zzzyyylllty.kangeldungeon.data.DataUtil
import io.github.zzzyyylllty.kangeldungeon.data.getProfile
import io.github.zzzyyylllty.kangeldungeon.function.kether.getBukkitPlayer
import io.github.zzzyyylllty.kangeldungeon.util.CastHelper
import taboolib.common5.Coerce
import taboolib.expansion.getDataContainer
import taboolib.library.kether.ArgTypes
import taboolib.library.kether.ParsedAction
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

/**
 * Chemdah
 * ink.ptms.chemdah.module.kether.ActionKAngelDungeon
 *
 * @author sky
 * @since 2021/2/10 6:39 下午
 */
class ActionKAngelDungeon {

    class KAngelDungeonDataGet(val key: ParsedAction<*>, val default: ParsedAction<*> = ParsedAction.noop<Any>()) : ScriptAction<Any>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Any> {
            val future = CompletableFuture<Any>()
            frame.newFrame(key).run<Any>().thenApply {
                frame.newFrame(default).run<Any>().thenApply { def ->
                    future.complete(DataUtil.getDataRaw(frame.getBukkitPlayer(), it.toString()) ?: def)
                }
            }
            return future
        }
    }

    class KAngelDungeonDataSet(
        val key: ParsedAction<*>,
        val value: ParsedAction<*>,
        val symbol: PlayerOperator.Method,
        val default: ParsedAction<*> = ParsedAction.noop<Any>(),
    ) : ScriptAction<Void>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Void> {
            return frame.newFrame(key).run<Any>().thenAccept { key ->
                frame.newFrame(value).run<Any>().thenAccept { value ->
                    frame.newFrame(default).run<Any>().thenAccept { def ->
                        val persistentDataContainer = frame.getBukkitPlayer().getDataContainer()
                        when {
                            value == null -> {
                                persistentDataContainer.delete(key.toString())
                            }
                            symbol == PlayerOperator.Method.INCREASE -> {
                                CastHelper.increaseAny(CastHelper.smartCast(persistentDataContainer[key.toString()]) ?: def, value)
                                    ?.let { persistentDataContainer[key.toString()] = it }
                            }
                            else -> {
                                persistentDataContainer[key.toString()] = value
                            }
                        }
                    }
                }
            }
        }
    }

    class KAngelDungeonDataKeys : ScriptAction<List<String>>() {

        override fun run(frame: ScriptFrame): CompletableFuture<List<String>> {
            return CompletableFuture.completedFuture(frame.getProfile().data.keys().toList())
        }
    }

    class KAngelDungeonLevelSet(val key: ParsedAction<*>, val type: LevelType, val value: ParsedAction<*>, val symbol: PlayerOperator.Method) : ScriptAction<Void>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Void> {
            return frame.newFrame(key).run<Any>().thenAccept { key ->
                frame.newFrame(value).run<Any>().thenAccept { value ->
                    val levelName = key.toString()
                    val playerKAngelDungeon = frame.getProfile()
                    val value = value.toString()
                    if (symbol == PlayerOperator.Method.INCREASE) {
                        if (type == LevelType.LEVEL) {
                            playerKAngelDungeon.addLevel(levelName, value.toLong())
                        } else {
                            playerKAngelDungeon.addExp(levelName, value.toDouble())
                        }
                    } else {
                        if (type == LevelType.LEVEL) {
                            playerKAngelDungeon.setLevel(levelName, value.toLong())
                        } else {
                            playerKAngelDungeon.setExp(levelName, value.toDouble())
                        }
                    }
                }
            }
        }
    }

    class KAngelDungeonLevelGet(val key: ParsedAction<*>, val type: LevelType) : ScriptAction<Any>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Any> {
            val future = CompletableFuture<Any>()
            frame.newFrame(key).run<Any>().thenApply {
                val levelName = it.toString()
                when (type) {
                    LevelType.LEVEL -> {
                        future.complete(frame.getProfile().getLevel(levelName))
                    }
                    LevelType.EXP -> {
                        future.complete(frame.getProfile().getExp(levelName))
                    }
                    LevelType.NEXT_EXP -> {
                        future.complete(frame.getProfile().getNextLevelExp(levelName))
                    }
                }
            }
            return future
        }
    }

    enum class LevelType {

        LEVEL, EXP, NEXT_EXP
    }

    companion object {

        /**
         * quser data *key
         * quser data *key to *value
         * quser data *key add *value
         * quser data keys
         *
         * quser level *default level
         * quser level *default level to *100
         * quser level *default exp
         * quser level *default exp add *100
         */
        @KetherParser(["quser"], shared = true)
        fun parser() = scriptParser {
            when (it.expects("data", "level")) {
                "data" -> {
                    try {
                        it.mark()
                        it.expect("keys")
                        KAngelDungeonDataKeys()
                    } catch (ex: Throwable) {
                        it.reset()
                        val key = it.next(ArgTypes.ACTION)
                        try {
                            it.mark()
                            when (it.expects("+", "=", "to", "add", "increase")) {
                                "=", "to" -> KAngelDungeonDataSet(key, it.next(ArgTypes.ACTION), PlayerOperator.Method.MODIFY)
                                "+", "add", "increase" -> {
                                    val value = it.next(ArgTypes.ACTION)
                                    try {
                                        it.mark()
                                        it.expect("default")
                                        KAngelDungeonDataSet(key, value, PlayerOperator.Method.INCREASE, it.next(ArgTypes.ACTION))
                                    } catch (ex: Throwable) {
                                        it.reset()
                                        KAngelDungeonDataSet(key, value, PlayerOperator.Method.INCREASE)
                                    }
                                }

                                else -> error("out of case")
                            }
                        } catch (ex: Throwable) {
                            it.reset()
                            try {
                                it.mark()
                                it.expect("default")
                                KAngelDungeonDataGet(key, it.next(ArgTypes.ACTION))
                            } catch (ex: Throwable) {
                                it.reset()
                                KAngelDungeonDataGet(key)
                            }
                        }
                    }
                }

                "level" -> {
                    val key = it.next(ArgTypes.ACTION)
                    val type = when (it.expects("level", "exp", "exp-max")) {
                        "level" -> LevelType.LEVEL
                        "exp" -> LevelType.EXP
                        "next", "next_exp", "required" -> LevelType.NEXT_EXP
                        else -> error("out of case")
                    }
                    try {
                        it.mark()
                        val method = when (it.expects("+", "=", "to", "add", "increase")) {
                            "=", "to" -> PlayerOperator.Method.MODIFY
                            "+", "add", "increase" -> PlayerOperator.Method.INCREASE
                            else -> error("out of case")
                        }
                        KAngelDungeonLevelSet(key, type, it.next(ArgTypes.ACTION), method)
                    } catch (ex: Throwable) {
                        it.reset()
                        KAngelDungeonLevelGet(key, type)
                    }
                }

                else -> error("out of case")
            }
        }
    }
}