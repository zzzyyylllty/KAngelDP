package io.github.zzzyyylllty.kangeldungeon.util

import org.bukkit.Bukkit


val dependencies = listOf(
    "adventure",
    // "arim",
    "caffeine",
    "fluxon",
    // "graaljs",
    "gson",
    "jackson",
    // "kotlincrypto",
)

object DependencyHelper {

    val mmLib by lazy {
        isPluginInstalled("MythicLib")
    }

    val papi by lazy {
        isPluginInstalled("PlaceholderAPI")
    }

    val pe by lazy {
        isPluginInstalled("packetevents")
    }

    val ce by lazy {
        isPluginInstalled("CraftEngine")
    }

    val ia by lazy {
        isPluginInstalled("ItemsAdder")
    }

    val ox by lazy {
        isPluginInstalled("Oxaren")
    }

    val ne by lazy {
        isPluginInstalled("Nexo")
    }




    fun isPluginInstalled(name: String): Boolean {
        return (Bukkit.getPluginManager().getPlugin(name) != null)
    }

}


