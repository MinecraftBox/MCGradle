package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import net.minecraftforge.mergetool.AnnotationVersion
import net.minecraftforge.mergetool.Merger
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskMergeJars : BaseTask(SPLIT_SERVER, DOWNLOAD_CLIENT) {
    @InputFile
    lateinit var clientJar: File

    @InputFile
    lateinit var serverJar: File

    @OutputFile
    lateinit var mergedJar: File

    @TaskAction
    fun merge() {
        val merger = Merger(clientJar, serverJar, mergedJar)
        merger.annotate(
            if (project.newConfig) {
                AnnotationVersion.API
            } else {
                AnnotationVersion.NMF
            }
        )
        merger.process()
    }

    override fun setup() {
        clientJar = File(project.string(CLIENT_JAR))
        serverJar = File(project.string(SPLIT_SERVER_JAR))
        mergedJar = File(project.string(MERGED_JAR))
    }
}