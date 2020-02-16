package club.ampthedev.mcgradle.user.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.TaskRecompile
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.user.utils.START_LIB_LOCATION
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class TaskGenerateStartLib : BaseTask(
        DOWNLOAD_NATIVES, DOWNLOAD_ASSETS) {
    @Input
    fun getTemplate() = StartSourceGenerator.template

    @Input
    fun getOptions() = StartSourceGenerator.options

    @InputFiles
    lateinit var classpath: FileCollection

    @OutputFile
    lateinit var library: File

    @TaskAction
    fun generate() {
        val sourceDir = File(temporaryDir, "sources")
        sourceDir.deleteRecursively()
        val classes = File(temporaryDir, "classes")
        classes.deleteRecursively()
        val sourceFile = File(sourceDir, "club/ampthedev/mcgradle/Start.java")
        prepareDirectory(sourceFile.parentFile)
        prepareDirectory(classes)
        sourceFile.bufferedWriter().use {
            it.write(StartSourceGenerator.generate())
        }
        TaskRecompile.updateExtDirs()
        ant.invokeMethod(
            "javac",
            mapOf(
                "srcDir" to sourceDir.canonicalPath,
                "destDir" to classes.canonicalPath,
                "failonerror" to true,
                "includeantruntime" to false,
                "classpath" to classpath.asPath,
                "encoding" to "utf-8",
                "source" to "1.8",
                "target" to "1.8",
                "debug" to true
            )
        )
        prepareDirectory(library.parentFile)
        val entriesAdded = hashSetOf<String>()
        JarOutputStream(library.outputStream()).use { out ->
            project.fileTree(classes).visit {
                if (it.isDirectory) return@visit
                val path = it.relativePath.toString().replace('\\', '/')
                if (entriesAdded.contains(path)) return@visit
                entriesAdded.add(path)
                out.putNextEntry(ZipEntry(path))
                it.copyTo(out)
            }
        }
    }

    override fun setup() {
        if (!::library.isInitialized) library = project.mcgFile(START_LIB_LOCATION)
        if (!::classpath.isInitialized) classpath = project.configurations.getByName(CONFIGURATION_MC_DEPS)
    }
}