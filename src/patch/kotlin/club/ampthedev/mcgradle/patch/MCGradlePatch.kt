package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BasePlugin
import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.impl.*
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.patch.tasks.TaskCopySource
import club.ampthedev.mcgradle.patch.tasks.TaskGeneratePatches
import club.ampthedev.mcgradle.patch.utils.*
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
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
        task(DOWNLOAD_NATIVES, TaskDownloadNatives::class)
        task(SOURCE_DEOBF, TaskSourceDeobf::class)
        task(APPLY_MOD_PATCHES, TaskApplyPatches::class) {
            input = project.mcgFile(SOURCE_MAPPED_JAR)
            output = File(temporaryDir, MOD_PATCHED_JAR)
            patches = extension.patchDir
            prepareDirectory(patches)
            dependsOn(SOURCE_DEOBF)
        }
        task(DOWNLOAD_ASSET_INDEX, TaskDownloadAssetIndex::class)
        task(DOWNLOAD_ASSETS, TaskDownloadAssets::class)
        task(COPY_SOURCES, TaskCopySource::class)
        task(GENERATE_PATCHES, TaskGeneratePatches::class)
        task(RECOMPILE_CLEAN_TASK, TaskRecompile::class)
        val setupTask = project.task(SETUP)
        setupTask.group = TaskType.MAIN.groupName
        setupTask.dependsOn(COPY_SOURCES, DOWNLOAD_NATIVES, DOWNLOAD_ASSETS)
    }

    override fun setup(project: Project) {
        super.setup(project)
        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSets = javaPlugin.sourceSets
        val main = extension.sourceSet ?: sourceSets.getByName("main")
        val extraSrcRoot = (project.tasks.getByName(COPY_SOURCES) as TaskCopySource).outputDir
        main.java.srcDirs(File(extraSrcRoot, "java"))
        main.resources.srcDirs(File(extraSrcRoot, "resources"))
        project.configurations.getByName(main.compileConfigurationName)
            .extendsFrom(project.configurations.getByName(CONFIGURATION_MC_DEPS))
    }
}