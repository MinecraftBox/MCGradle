package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.VersionJsonDownloadTask
import club.ampthedev.mcgradle.base.utils.SERVER_JAR
import club.ampthedev.mcgradle.base.utils.string
import com.google.gson.JsonObject
import org.gradle.api.tasks.OutputFile
import java.io.File

open class TaskDownloadServer : VersionJsonDownloadTask() {
    @OutputFile
    override var dest: File? = null

    override fun JsonObject.getDownloadObject(): JsonObject = getAsJsonObject("downloads").getAsJsonObject("server")

    override fun setup() {
        dest = File(project.string(SERVER_JAR))
    }
}