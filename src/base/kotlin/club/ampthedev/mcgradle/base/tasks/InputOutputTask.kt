package club.ampthedev.mcgradle.base.tasks

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import java.io.File

abstract class InputOutputTask(type: TaskType = TaskType.OTHER, vararg deps: String) : BaseTask(type, *deps) {
    @InputFile
    var input: File? = null

    @OutputFile
    var output: File? = null

    // Convenience methods for tasks
    // because ?. can get annoying.
    // Gradle already doesn't accept
    // null values for input and output.

    protected fun getIn() = input!!

    protected fun getOut() = output!!
}