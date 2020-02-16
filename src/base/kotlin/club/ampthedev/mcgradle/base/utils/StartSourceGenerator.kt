package club.ampthedev.mcgradle.base.utils

import groovy.json.StringEscapeUtils
import org.gradle.api.Project

object StartSourceGenerator {
    val options = hashMapOf<String, String>()
    lateinit var template: String
        private set

    fun init(project: Project) {
        template = StartSourceGenerator::class.java.getResourceAsStream("/classes/Start.java").bufferedReader()
            .use { it.readText() }
        options["runDirectory"] = project.mcgFile(RUN_DIRECTORY).absolutePath
        options["nativeDirectory"] = project.mcgFile(NATIVES_DIRECTORY).absolutePath
        options["clientMainClass"] = project.string(project.plugin.extension.clientMainClass)
        options["serverMainClass"] = project.string(project.plugin.extension.serverMainClass)
        options["assetsDirectory"] = project.mcgFile(ASSETS_DIRECTORY).absolutePath
        options["assetIndex"] = project.getVersionJson().getAsJsonObject("assetIndex")["id"].asString
    }

    fun generate(): String {
        var rv = template
        for (option in options) {
            rv = rv.replace("\${${option.key}}", StringEscapeUtils.escapeJava(option.value))
        }
        return rv
    }
}