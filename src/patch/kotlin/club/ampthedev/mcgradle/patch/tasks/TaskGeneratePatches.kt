package club.ampthedev.mcgradle.patch.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.utils.SOURCE_DEOBF
import club.ampthedev.mcgradle.base.utils.SOURCE_MAPPED_JAR
import club.ampthedev.mcgradle.base.utils.mcgFile
import club.ampthedev.mcgradle.base.utils.prepareDirectory
import club.ampthedev.mcgradle.patch.utils.SOURCE_DIR
import com.cloudbees.diff.Diff
import com.cloudbees.diff.Hunk
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

open class TaskGeneratePatches : BaseTask(TaskType.MAIN, SOURCE_DEOBF) {
    @InputDirectory
    lateinit var input: File

    @InputFile
    lateinit var clean: File

    @OutputDirectory
    var patches = File(project.projectDir, "patches")

    private val created = hashSetOf<File>()

    @TaskAction
    fun generate() {
        created.clear()
        prepareDirectory(patches)
        processFiles()
        removeOldFiles()
    }

    private fun processFiles() {
        ZipFile(clean).use {
            for (entry in it.entries()) {
                if (File(input, entry.name).exists()) {
                    it.getInputStream(entry).use { z ->
                        File(input, entry.name).inputStream().use { f ->
                            processOriginal(entry.name, z, f)
                        }
                    }
                }
            }
        }
    }

    private fun processOriginal(name: String, original: InputStream, changed: InputStream) {
        val patchFile = File(patches, name.replace('\\', '/').replace('/', '.') + ".patch").canonicalFile
        val oData = original.readBytes()
        val cData = changed.readBytes()
        val diff = Diff.diff(oData.inputStream().reader(), cData.inputStream().reader(), false)
        val actualName = if (!name.startsWith("/")) {
            "/$name"
        } else {
            name
        }
        if (!diff.isEmpty()) {
            val diffStr = diff.toUnifiedDiff(
                "original$actualName", "changed$actualName",
                oData.inputStream().reader(),
                cData.inputStream().reader(),
                3
            ).replace("\r\n", "\n")
                .replace("\n${Hunk.ENDING_NEWLINE}", "\n")
            patchFile.bufferedWriter().use {
                it.write(diffStr)
            }
            created.add(patchFile)
        }
    }

    private fun removeOldFiles() {
        for (file in patches.listFiles() ?: error("")) {
            if (!created.contains(file)) {
                file.delete()
            }
        }
    }

    override fun setup() {
        if (!::input.isInitialized) input = project.mcgFile("$SOURCE_DIR/java")
        if (!::clean.isInitialized) clean = project.mcgFile(SOURCE_MAPPED_JAR)
    }
}