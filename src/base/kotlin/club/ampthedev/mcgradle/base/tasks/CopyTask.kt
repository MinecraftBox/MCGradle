package club.ampthedev.mcgradle.base.tasks

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import club.ampthedev.mcgradle.base.utils.prepareDirectory

class CopyTask(type: TaskType = TaskType.OTHER, vararg deps: String) : InputOutputTask(type, *deps) {
    @TaskAction
    fun copy() {
        logger.info("Copying ${getIn().path} to ${getOut().absolutePath}")
        prepareDirectory(getOut().parentFile)
        FileUtils.copyFile(getIn(), getOut())
    }
}