package club.ampthedev.mcgradle.user.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.TaskMergeJars
import club.ampthedev.mcgradle.base.utils.castTo
import club.ampthedev.mcgradle.base.utils.get
import club.ampthedev.mcgradle.base.utils.mcgFile
import club.ampthedev.mcgradle.base.utils.prepareDirectory
import club.ampthedev.mcgradle.user.utils.*
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

open class TaskInjectClasses : BaseTask() {
    @InputFile
    lateinit var mcJar: File

    @InputFile
    lateinit var modJar: File

    @OutputFile
    lateinit var output: File

    @TaskAction
    fun inject() {
        prepareDirectory(output.parentFile)
        ZipFile(mcJar).use { mc ->
            ZipFile(modJar).use { mod ->
                ZipOutputStream(output.outputStream()).use { out ->
                    for (entry in mc.entries()) {
                        if (entry.isDirectory) continue
                        if (entry.name.startsWith("META-INF/")) continue
                        out.putNextEntry(ZipEntry(entry.name))
                        out.write(mc.getInputStream(entry).use { it.readBytes() })
                    }
                    for (entry in mod.entries()) {
                        if (entry.isDirectory) continue
                        if (entry.name
                                .startsWith("META-INF/") || entry.name == "binpatches.lzma" || entry.name == "binpatches.dev.lzma"
                        ) continue
                        out.putNextEntry(ZipEntry(entry.name))
                        out.write(mod.getInputStream(entry).use { it.readBytes() })
                    }
                }
            }
        }
    }

    override fun setup() {
        if (!project.vanilla) {
            if (!::mcJar.isInitialized) mcJar = project.mcgFile(PATCHED_MERGED)
            if (!::modJar.isInitialized) modJar = project.mcgFile(MOD_JAR)
            if (!::output.isInitialized) output = project.mcgFile(PATCHED_INJECTED)
        }
    }
}