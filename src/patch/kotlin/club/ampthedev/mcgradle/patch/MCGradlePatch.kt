package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BasePlugin
import club.ampthedev.mcgradle.base.tasks.impl.*
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.Project
import java.io.File

class MCGradlePatch : BasePlugin<PatchExtension>() {
    override val extension by lazy { PatchExtension(project) }

    override fun preSetup(project: Project) {
        super.preSetup(project)
        task(DOWNLOAD_CLIENT, TaskDownloadClient::class)
        task(DOWNLOAD_SERVER, TaskDownloadServer::class)
        task(SPLIT_SERVER, TaskSplitServer::class)
        task(MERGE_JARS, TaskMergeJars::class)
        task(GENERATE_MAPPINGS, TaskGenerateMappings::class)
        task(DEOBF_JAR, TaskDeobf::class)
        task(DECOMP, TaskDecomp::class)
        task(APPLY_MCP_PATCHES, TaskApplyPatches::class) {
            input = File(project.string(DECOMP_JAR))
            output = File(project.string(PATCHED_JAR))
            patches = File(project.string(if (project.newConfig) MCP_PATCHES_NEW else MCP_PATCHES))
            mcpPatch = true
            // dependsOn(DECOMP)
        }
        task(SOURCE_DEOBF, TaskSourceDeobf::class)
    }
}