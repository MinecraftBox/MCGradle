package club.ampthedev.mcgradle.base.tasks.downloading

import club.ampthedev.mcgradle.base.tasks.VersionJsonDownloadTask
import club.ampthedev.mcgradle.base.utils.CLIENT_JAR
import club.ampthedev.mcgradle.base.utils.string
import com.google.gson.JsonObject
import org.gradle.api.tasks.OutputFile
import java.io.File

open class TaskDownloadClient : VersionJsonDownloadTask() {
    @OutputFile
    override val dest = File(project.string(CLIENT_JAR))

    override fun JsonObject.getDownloadObject(): JsonObject = getAsJsonObject("downloads").getAsJsonObject("client")
}