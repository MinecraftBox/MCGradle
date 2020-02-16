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
    lateinit var properties: Map<String, Any>

    @Input
    fun getOptions() = StartSourceGenerator.options

    @Input
    fun getTemplate() = StartSourceGenerator.template

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

        startClass.writer().use {
            it.write(StartSourceGenerator.generate())
        }

        var propertiesSource = """
            package club.ampthedev.mcgradle;
            
            public final class Properties {
                private Properties() {
                }


        """.trimIndent()

        for (property in properties) {
            propertiesSource += "    public static final String ${property.key} = \"${StringEscapeUtils.escapeJava(
                property.value.toString()
            )}\";\n"
        }
        propertiesSource += "}\n"
        val propertiesSourceFile = File(sourceDir, "club/ampthedev/mcgradle/Properties.java")
        prepareDirectory(propertiesSourceFile.parentFile)
        propertiesSourceFile.bufferedWriter().use {
            it.write(propertiesSource)
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
        if (!::properties.isInitialized) properties = project.plugin.extension.properties
    }
}