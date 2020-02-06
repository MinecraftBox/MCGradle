package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BasePlugin
import club.ampthedev.mcgradle.base.tasks.downloading.TaskDownloadClient
import club.ampthedev.mcgradle.base.utils.task
import org.gradle.api.Project

class MCGradlePatch : BasePlugin<PatchExtension>() {
    override val extension: PatchExtension
        get() = PatchExtension(project)

    override fun setup(project: Project) {
        super.setup(project)
        project.task("downloadClient", TaskDownloadClient::class)
    }
}