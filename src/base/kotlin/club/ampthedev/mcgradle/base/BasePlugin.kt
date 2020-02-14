package club.ampthedev.mcgradle.base

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.reflect.KClass

abstract class BasePlugin<T : BaseExtension> : Plugin<Project> {
    protected abstract val extension: T
    protected lateinit var project: Project
    private val tasks = hashMapOf<String, Pair<BaseTask, BaseTask.() -> Unit>>()

    open fun preSetup(project: Project) {
        this.project = project
        project.plugin = this
        project.extensions.add(EXTENSION_NAME, extension)
        project.configurations.create(CONFIGURATION_MC_DEPS)
        project.configurations.create(CONFIGURATION_MCP_MAPS)
        project.configurations.create(CONFIGURATION_MCP_DATA)
    }

    fun <T : BaseTask> task(name: String, task: KClass<T>, setup: T.() -> Unit = {}) {
        tasks[name] = Pair(project.tasks.create(name, task.java), castTo(setup))
    }

    open fun setup(project: Project) {
        project.repositories.mavenCentral()
        project.repositories.maven { it.setUrl("https://files.minecraftforge.net/maven") }
        project.repositories.maven { it.setUrl("https://libraries.minecraft.net") }

        project.addReplacements(mapOf(
                CACHE_DIR to "${project.gradle.gradleUserHomeDir.absolutePath}/caches/mcgradle",
                PROJECT_DIR to project.projectDir.absolutePath,
                MC_VERSION to extension.version,
                MAPPING_CHANNEL to extension.mappingChannel,
                MAPPING_VERSION to extension.mappingVersion,
                RUN_DIRECTORY to extension.runDirectory,
                BUILD_DIR to project.buildDir.absolutePath
        ))

        // Add dependencies
        for (element in project.getVersionJson().getAsJsonArray("libraries")) {
            val obj = element.asJsonObject
            if (shouldIncludeDependency(obj)) {
                project.dependencies.add(CONFIGURATION_MC_DEPS, getDependencyString(obj))
            }
        }

        // Add MCP stuff
        project.dependencies.add(CONFIGURATION_MCP_DATA, mapOf(
                "group" to "de.oceanlabs.mcp",
                "name" to "mcp_${extension.mappingChannel}",
                "version" to "${extension.mappingVersion}-${extension.version}",
                "ext" to "zip"
        ))

        project.dependencies.add(CONFIGURATION_MCP_MAPS, mapOf(
                "group" to "de.oceanlabs.mcp",
                "name" to "mcp",
                "version" to extension.version,
                "classifier" to "srg",
                "ext" to "zip"
        ))

        // Set up tasks
        for (task in tasks) {
            task.value.first.setup()
            task.value.second(task.value.first)
        }
    }

    override fun apply(target: Project) {
        preSetup(target)
        target.afterEvaluate {
            setup(it)
        }
    }
}