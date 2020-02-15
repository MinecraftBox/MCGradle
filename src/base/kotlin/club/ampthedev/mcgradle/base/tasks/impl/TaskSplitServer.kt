package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.ZipEditTask
import club.ampthedev.mcgradle.base.utils.DOWNLOAD_SERVER
import club.ampthedev.mcgradle.base.utils.SERVER_JAR
import club.ampthedev.mcgradle.base.utils.SPLIT_SERVER_JAR
import club.ampthedev.mcgradle.base.utils.string
import java.io.File
import java.util.zip.ZipEntry

open class TaskSplitServer : ZipEditTask(TaskType.OTHER, DOWNLOAD_SERVER) {
    override fun ZipEntry.shouldInclude(): Boolean {
        if (isDirectory) return false
        if (!(name.endsWith(".class") || name.endsWith(".java") || name.startsWith("org/"))) {
            return true
        }
        if (name.contains("/")) {
            if (!name.startsWith("net/minecraft")) {
                return false
            }
        }
        return true
    }

    override fun setup() {
        input = File(project.string(SERVER_JAR))
        output = File(project.string(SPLIT_SERVER_JAR))
    }
}