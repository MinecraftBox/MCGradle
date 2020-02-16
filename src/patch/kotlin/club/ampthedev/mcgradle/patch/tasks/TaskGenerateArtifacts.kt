package club.ampthedev.mcgradle.patch.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.TaskDeobf
import club.ampthedev.mcgradle.base.utils.MERGED_JAR
import club.ampthedev.mcgradle.base.utils.MERGE_JARS
import club.ampthedev.mcgradle.base.utils.mcgFile
import club.ampthedev.mcgradle.base.utils.prepareDirectory
import club.ampthedev.mcgradle.patch.utils.GENERATE_BIN_PATCHES
import club.ampthedev.mcgradle.patch.utils.REOBFUSCATE_JAR
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

open class TaskGenerateArtifacts :
    BaseTask(REOBFUSCATE_JAR, GENERATE_BIN_PATCHES, MERGE_JARS) {
    @InputFile
    lateinit var reobfuscated: File

    @InputFile
    lateinit var original: File

    @InputFile
    lateinit var runBinPatches: File

    @InputFile
    lateinit var devBinPatches: File

    @OutputFile
    lateinit var output: File

    @TaskAction
    fun generate() {
        prepareDirectory(output.parentFile)
        ZipFile(reobfuscated).use { reobf ->
            ZipFile(original).use { original ->
                ZipOutputStream(output.outputStream()).use { output ->
                    val entries = reobf.stream().filter {
                        !it.name
                            .startsWith("club/ampthedev/mcgradle/Start") && it.name != "club/ampthedev/mcgradle/Properties.class"
                    }
                    for (entry in entries) {
                        if (entry.isDirectory) continue
                        if (original.getEntry(entry.name) != null) continue
                        val newEntry = ZipEntry(entry.name)
                        output.putNextEntry(newEntry)
                        output.write(reobf.getInputStream(entry).use { it.readBytes() })
                    }
                    output.putNextEntry(ZipEntry("binpatches.lzma"))
                    output.write(runBinPatches.inputStream().use { it.readBytes() })
                    output.putNextEntry(ZipEntry("binpatches.dev.lzma"))
                    output.write(devBinPatches.inputStream().use { it.readBytes() })
                }
            }
        }
    }

    override fun setup() {
        if (!::reobfuscated.isInitialized) reobfuscated = (project.tasks.getByName(REOBFUSCATE_JAR) as TaskDeobf).out
        if (!::original.isInitialized) original = project.mcgFile(MERGED_JAR)
        val genBinPatches = project.tasks.getByName(GENERATE_BIN_PATCHES) as TaskGenerateBinPatches
        if (!::runBinPatches.isInitialized) runBinPatches = genBinPatches.runBinPatches
        if (!::devBinPatches.isInitialized) devBinPatches = genBinPatches.devBinPatches
        if (!::output.isInitialized) output = (project.tasks.getByName("jar") as Jar).archiveFile.get().asFile
    }
}