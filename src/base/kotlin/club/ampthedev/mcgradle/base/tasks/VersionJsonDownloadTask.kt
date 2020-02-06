package club.ampthedev.mcgradle.base.tasks

import club.ampthedev.mcgradle.base.utils.getVersionJson
import com.google.gson.JsonObject
import org.gradle.api.tasks.Input

abstract class VersionJsonDownloadTask(type: TaskType = TaskType.OTHER, vararg deps: String) : BasicDownloadTask(type, *deps) {
    override val url: String
        @Input get() = project.getVersionJson().getDownloadObject().get("url").asString

    override val sha1: String?
        @Input get() = project.getVersionJson().getDownloadObject().get("sha1")?.asString

    protected abstract fun JsonObject.getDownloadObject(): JsonObject
}