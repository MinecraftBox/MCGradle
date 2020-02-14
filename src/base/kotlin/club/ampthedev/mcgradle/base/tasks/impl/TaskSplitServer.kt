package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.ZipEditTask
import club.ampthedev.mcgradle.base.utils.DOWNLOAD_SERVER
import club.ampthedev.mcgradle.base.utils.SERVER_JAR
import club.ampthedev.mcgradle.base.utils.SPLIT_SERVER_JAR
import club.ampthedev.mcgradle.base.utils.string
import java.io.File

open class TaskSplitServer : ZipEditTask(TaskType.OTHER, DOWNLOAD_SERVER) {
    override val pattern = "[^/]+|net/minecraft/.+".toRegex()

    override fun setup() {
        input = File(project.string(SERVER_JAR))
        output = File(project.string(SPLIT_SERVER_JAR))
    }
}