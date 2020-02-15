package club.ampthedev.mcgradle.patch.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.TaskApplyPatches
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.patch.utils.APPLY_MOD_PATCHES
import club.ampthedev.mcgradle.patch.utils.SOURCE_DIR
import groovy.json.StringEscapeUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskCopySource : BaseTask(APPLY_MOD_PATCHES) {
    @InputFile
    lateinit var sourceJar: File

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    val options = hashMapOf<String, String>()

    @Input
    val template =
        TaskCopySource::class.java.getResourceAsStream("/classes/Start.java").bufferedReader().use { it.readText() }

    @TaskAction
    fun copy() {
        val sourceDir = File(outputDir, "java")
        val resourceDir = File(outputDir, "resources")
        project.zipTree(sourceJar).visit {
            if (!it.isDirectory) {
                val name = it.name
                val path = it.path
                if (path.startsWith("META-INF/") || name.startsWith("Log4j-") || name.endsWith(".der")) return@visit
                val outputDir = if (name.endsWith(".java")) {
                    sourceDir
                } else {
                    resourceDir
                }
                val output = File(outputDir, path)
                prepareDirectory(output.parentFile)
                it.copyTo(output)
            }
        }
        val startClass = File(sourceDir, "club/ampthedev/mcgradle/Start.java")
        prepareDirectory(startClass.parentFile)
        var source = template
        for (option in options) {
            source = source.replace("\${${option.key}}", StringEscapeUtils.escapeJava(option.value))
        }
        startClass.writer().use {
            it.write(source)
        }
    }

    override fun setup() {
        if (!::outputDir.isInitialized) {
            outputDir = project.mcgFile(SOURCE_DIR)
        }
        if (!::sourceJar.isInitialized) {
            val task = project.tasks.getByName(APPLY_MOD_PATCHES) as TaskApplyPatches
            sourceJar = task.output!!
        }
        options["runDirectory"] = project.string(RUN_DIRECTORY)
        options["nativeDirectory"] = project.string(NATIVES_DIRECTORY)
        options["clientMainClass"] = project.string(project.plugin.extension.clientMainClass)
        options["serverMainClass"] = project.string(project.plugin.extension.serverMainClass)
        options["assetsDirectory"] = project.string(ASSETS_DIRECTORY)
        options["assetIndex"] = project.getVersionJson().getAsJsonObject("assetIndex")["id"].asString
    }
}