package club.ampthedev.mcgradle.user

import club.ampthedev.mcgradle.base.BasePlugin
import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.impl.*
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.user.tasks.TaskApplyBinaryPatches
import club.ampthedev.mcgradle.user.tasks.TaskGenerateStartLib
import club.ampthedev.mcgradle.user.tasks.TaskInjectClasses
import club.ampthedev.mcgradle.user.utils.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.util.*
import java.util.zip.ZipFile

class MCGradleUser : BasePlugin<UserExtension>() {
    override val extension by lazy { UserExtension(project) }

    override fun preSetup(project: Project) {
        super.preSetup(project)
        project.configurations.create(CONFIGURATION_MOD)
        task(GENERATE_MAPPINGS, TaskGenerateMappings::class)
        task(DOWNLOAD_CLIENT, TaskDownloadClient::class)
        task(DOWNLOAD_SERVER, TaskDownloadServer::class)
        task(SPLIT_SERVER, TaskSplitServer::class)
        task(BINPATCH_CLIENT, TaskApplyBinaryPatches::class) {
            if (project.vanilla) return@task
            input = project.mcgFile(CLIENT_JAR)
            output = project.mcgFile(PATCHED_CLIENT)
            patchLzmaPath = "binpatches.lzma"
            patchRoot = "client"
            dependsOn(DOWNLOAD_CLIENT)
        }
        task(BINPATCH_SERVER, TaskApplyBinaryPatches::class) {
            if (project.vanilla) return@task
            input = project.mcgFile(SPLIT_SERVER_JAR)
            output = project.mcgFile(PATCHED_SERVER)
            patchLzmaPath = "binpatches.lzma"
            patchRoot = "server"
            dependsOn(SPLIT_SERVER)
        }
        task(MERGE_PATCHED_JARS, TaskMergeJars::class) {
            if (!project.vanilla) {
                clientJar = project.mcgFile(PATCHED_CLIENT)
                serverJar = project.mcgFile(PATCHED_SERVER)
                mergedJar = project.mcgFile(PATCHED_MERGED)
                dependsOn(BINPATCH_CLIENT, BINPATCH_SERVER)
            }
        }
        task(INJECT_CLASSES, TaskInjectClasses::class)
        task(MCP_DEOBF, TaskDeobf::class) {
            if (!project.vanilla) {
                jar = project.mcgFile(PATCHED_INJECTED)
                dependsOn(INJECT_CLASSES)
            }
            out = project.mcgFile(DEOBF_MCP)
            srg = project.mcgFile(NOTCH_MCP)
        }
        task(GEN_START, TaskGenerateStartLib::class)
        task(GEN_CLIENT_RUN, TaskCreateRunConfig::class) {
            configName = "Minecraft Client"
            mainClass = "club.ampthedev.mcgradle.Start"
            vmOptions.addAll(extension.jvmargs)
            args.addAll(extension.args)
            workingDirectory = File(extension.runDirectory)
            beforeRunTasks.add(GEN_START)
        }
        task(GEN_SERVER_RUN, TaskCreateRunConfig::class) {
            configName = "Minecraft Server"
            mainClass = "club.ampthedev.mcgradle.Start"
            vmOptions.addAll(extension.jvmargs)
            args.add("--server")
            args.addAll(extension.args)
            workingDirectory = File(extension.runDirectory)
            beforeRunTasks.add(GEN_START)
        }
        val genConfigs = project.task(GEN_RUNS)
        genConfigs.group = TaskType.MAIN.groupName
        genConfigs.dependsOn(GEN_CLIENT_RUN, GEN_SERVER_RUN)
        project.tasks.getByName("idea").dependsOn(GEN_RUNS)
        task(DOWNLOAD_NATIVES, TaskDownloadNatives::class)
        task(DOWNLOAD_ASSET_INDEX, TaskDownloadAssetIndex::class)
        task(DOWNLOAD_ASSETS, TaskDownloadAssets::class)
    }

    override fun setup(project: Project) {
        project.repositories.mavenCentral()
        project.repositories.maven { it.setUrl("https://files.minecraftforge.net/maven") }
        project.repositories.maven { it.setUrl("https://libraries.minecraft.net") }
        var modJar: File? = null
        val conf = project.configurations.getByName(CONFIGURATION_MOD)
        var used = false
        for (f in conf) {
            used = true
            val isMod = try {
                ZipFile(f).use {
                    it.getEntry("binpatches.lzma") != null && it.getEntry("binpatches.dev.lzma") != null
                }
            } catch (e: Throwable) {
                continue
            }
            if (isMod) {
                if (modJar != null) {
                    throw GradleException("You can only have one $CONFIGURATION_MOD dependency")
                }
                modJar = f
            }
        }
        if (modJar == null && used) {
            throw GradleException("No valid $CONFIGURATION_MOD dependency declared")
        }
        if (modJar != null) {
            project.addReplacement(MOD_JAR, modJar.absolutePath)
            project.addReplacement(MOD_HASH, modJar.hash("SHA-256"))
        }
        project.addReplacement(
            REPO, if (project.vanilla) {
                VANILLA_REPO
            } else MOD_REPO
        )
        super.setup(project)

        project.repositories.maven { it.url = project.mcgFile(REPO).toURI() }

        project.dependencies.add("compile", project.files(castTo<TaskGenerateStartLib>(project.tasks.getByName(GEN_START)).library))
        val recompFile = project.mcgFile(DECOMP_BIN)
        when {
            recompFile.exists() -> {
                project.dependencies.add("compile", MCSRC_DEP)
            }
            project.mcgFile(DEOBF_MCP).exists() -> {
                project.dependencies.add("compile", MCBIN_DEP)
            }
            // !project.gradle.startParameter.taskNames.contains()
            else -> {
                println("Please run a setup task ASAP.")
            }
        }
        project.configurations.getByName("compile").extendsFrom(project.configurations.getByName(CONFIGURATION_MC_DEPS))
        // project.configurations.getByName("compile").files.add(castTo<TaskDeobf>(project[MCP_DEOBF]).out)
    }
}