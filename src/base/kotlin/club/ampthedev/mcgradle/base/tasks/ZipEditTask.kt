package club.ampthedev.mcgradle.base.tasks

import org.apache.commons.io.IOUtils
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Task that edits a zip file's entries
 */
abstract class ZipEditTask(type: TaskType = TaskType.OTHER, vararg deps: String) : InputOutputTask(type, *deps) {
    /**
     * Edits the zip file
     */
    @TaskAction
    private fun editZip() {
        ZipFile(getIn()).use { input ->
            ZipOutputStream(FileOutputStream(getOut())).use { output ->
                input.stream().forEach {
                    if (keepEmptyDirectories() && it.isDirectory) {
                        output.putNextEntry(it) // zip directories don't need to be added/removed explicitly
                    } else if (getPattern().matches(it.name)) {
                        val newEntry = ZipEntry(it.name)
                        newEntry.lastModifiedTime = it.lastModifiedTime
                        output.putNextEntry(it)
                        output.write(edit(it, IOUtils.toByteArray(input.getInputStream(it))))
                    }
                }
            }
        }
    }

    /**
     * Determines whether empty directories should be kept.
     */
    protected open fun keepEmptyDirectories() = false

    /**
     * Returns the Regex pattern used to determine if an entry gets in, based on its name.
     */
    protected open fun getPattern() = Regex(".*")

    /**
     * Edits an entry's bytes and returns the modified version
     */
    protected abstract fun edit(entry: ZipEntry, bytes: ByteArray): ByteArray
}