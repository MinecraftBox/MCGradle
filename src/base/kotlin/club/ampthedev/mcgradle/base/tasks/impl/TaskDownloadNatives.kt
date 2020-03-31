package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskDownloadNatives : BaseTask() {
    @Input
    lateinit var deps: Configuration
    @Input
    var versionJson: Int = 0
    @OutputDirectory
    lateinit var output: File

    @TaskAction
    fun download() {
        output.deleteRecursively()
        prepareDirectory(output)
        for (element in project.getVersionJson().getAsJsonArray("libraries")) {
            val obj = element.asJsonObject
            if (shouldIncludeDependency(obj)) {
                val data = getDependencyData(obj)
                if (data.third.second) {
                    val file = File(project.mcgFile(CACHE_DIR), "dependencies/${data.first.replace(':', '_')}")
                    if (file.exists()) {
                        project.zipTree(file).visit {
                            if (it.isDirectory) return@visit
                            val outputFile = File(output, it.path)
                            prepareDirectory(outputFile.parentFile)
                            it.copyTo(outputFile)
                        }
                    }
                }
            }
        }
    }

    override fun setup() {
        if (!::deps.isInitialized) deps = project.configurations.getByName(CONFIGURATION_MC_DEPS)
        versionJson = project.getVersionJson().hashCode()
        if (!::output.isInitialized) output = project.mcgFile(NATIVES_DIRECTORY)
    }
}