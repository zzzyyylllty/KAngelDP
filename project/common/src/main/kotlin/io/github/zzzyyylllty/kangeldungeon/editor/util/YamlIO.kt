package io.github.zzzyyylllty.kangeldungeon.editor.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import java.io.File

/**
 * YAML read/write utilities for the visual editor.
 * Uses the same Jackson infrastructure as multiExtensionLoader.
 */
object YamlIO {

    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)

    private val mapType = TypeFactory.defaultInstance().constructMapType(
        MutableMap::class.java,
        String::class.java,
        Any::class.java
    )

    /**
     * Load a YAML file into a mutable map. Returns empty map if file doesn't exist.
     */
    fun loadYaml(file: File): MutableMap<String, Any?> {
        if (!file.exists()) return mutableMapOf()
        return try {
            @Suppress("UNCHECKED_CAST")
            (mapper.readValue(file, mapType) as? MutableMap<String, Any?>) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    /**
     * Save a map to a YAML file with pretty printing.
     */
    fun saveYaml(file: File, data: Map<String, Any?>) {
        file.parentFile.mkdirs()
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    /**
     * Get the dungeon folder for a given dungeon name.
     */
    fun dungeonFolder(name: String): File =
        File(KAngelDungeon.dataFolder, "dungeon/$name")

    /**
     * Get a config file within a dungeon's subdirectory.
     */
    fun dungeonFile(dungeonName: String, category: String, fileName: String): File =
        File(dungeonFolder(dungeonName), "$category/$fileName")

    /**
     * List all dungeon directories.
     */
    fun listDungeons(): List<String> {
        val dir = File(KAngelDungeon.dataFolder, "dungeon")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
    }

    /**
     * List YAML files in a dungeon category directory.
     */
    fun listYamlFiles(dungeonName: String, category: String): List<File> {
        val dir = File(dungeonFolder(dungeonName), category)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension in setOf("yml", "yaml") }
            ?.sortedBy { it.name } ?: emptyList()
    }
}
