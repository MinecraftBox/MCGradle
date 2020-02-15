package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.VersionJsonDownloadTask
import club.ampthedev.mcgradle.base.utils.ASSETS_DIRECTORY
import club.ampthedev.mcgradle.base.utils.getVersionJson
import club.ampthedev.mcgradle.base.utils.mcgFile
import com.google.gson.JsonObject
import org.gradle.api.tasks.OutputFile
import java.io.File

open class TaskDownloadAssetIndex : VersionJsonDownloadTask(TaskType.OTHER) {
    @OutputFile
    override var dest: File? = null

    override fun JsonObject.getDownloadObject(): JsonObject = getAsJsonObject("assetIndex")

    override fun setup() {
        val assetDir = project.mcgFile(ASSETS_DIRECTORY)
        dest = File(
            assetDir,
            "indexes/${project.getVersionJson().getAsJsonObject("assetIndex")["id"].asString}.json"
        )
    }
}