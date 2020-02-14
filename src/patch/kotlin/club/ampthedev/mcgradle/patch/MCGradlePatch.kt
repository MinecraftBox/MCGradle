package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BasePlugin
import club.ampthedev.mcgradle.base.tasks.impl.*
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.Project

class MCGradlePatch : BasePlugin<PatchExtension>() {
    override val extension: PatchExtension
        get() = PatchExtension(project)

    override fun preSetup(project: Project) {
        super.preSetup(project)
        task(DOWNLOAD_CLIENT, TaskDownloadClient::class)
        task(DOWNLOAD_SERVER, TaskDownloadServer::class)
        task(SPLIT_SERVER, TaskSplitServer::class)
        task(MERGE_JARS, TaskMergeJars::class)
        task(GENERATE_MAPPINGS, TaskGenerateMappings::class)
        task(DEOBF_JAR, TaskDeobf::class)
    }
}