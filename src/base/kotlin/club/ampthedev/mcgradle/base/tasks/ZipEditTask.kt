package club.ampthedev.mcgradle.base.tasks

import com.google.common.io.ByteStreams
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Task that edits a zip file's entries
 */
abstract class ZipEditTask(type: TaskType = TaskType.OTHER, vararg deps: String) : InputOutputTask(type, *deps) {
    protected open val pattern = Regex(".*")
    protected open val keepEmptyDirectories = false

    /**
     * Edits the zip file
     */
    @TaskAction
    private fun editZip() {
        initTask()
        ZipFile(getIn()).use { file ->
            ZipOutputStream(BufferedOutputStream(FileOutputStream(getOut()))).use { output ->
                for (s in file.entries()) {
                    if (keepEmptyDirectories && s.isDirectory) {
                        output.putNextEntry(s) // zip directories don't need to be added/removed explicitly
                    } else if (s.shouldInclude()) {
                        val newEntry = ZipEntry(s.name)
                        output.putNextEntry(newEntry)
                        output.write(edit(s, file.getInputStream(s).use { it.readBytes() }))
                    }
                }
            }
        }
    }

    protected open fun ZipEntry.shouldInclude() = pattern.matches(name)

    protected open fun initTask() {}

    /**
     * Edits an entry's bytes and returns the modified version
     */
    protected open fun edit(entry: ZipEntry, bytes: ByteArray) = bytes
}