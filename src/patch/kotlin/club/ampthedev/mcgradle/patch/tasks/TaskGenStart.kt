package club.ampthedev.mcgradle.patch.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.StartSourceGenerator
import club.ampthedev.mcgradle.base.utils.mcgFile
import club.ampthedev.mcgradle.base.utils.plugin
import club.ampthedev.mcgradle.base.utils.prepareDirectory
import club.ampthedev.mcgradle.patch.utils.SOURCE_DIR
import groovy.json.StringEscapeUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskGenStart : BaseTask() {
    @OutputDirectory
    lateinit var outputDir: File

    @Input
    lateinit var properties: Map<String, Any>

    @Input
    fun getOptions() = StartSourceGenerator.options

    @Input
    fun getTemplate() = StartSourceGenerator.template

    @TaskAction
    fun generate() {
        val sourceDir = File(outputDir, "java")
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
        if (!::properties.isInitialized) properties = project.plugin.extension.properties
    }
}