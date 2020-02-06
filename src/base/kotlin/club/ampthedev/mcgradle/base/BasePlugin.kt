package club.ampthedev.mcgradle.base

import org.gradle.api.Plugin
import org.gradle.api.Project
import club.ampthedev.mcgradle.base.utils.*

abstract class BasePlugin<T : BaseExtension> : Plugin<Project> {
    protected abstract val extension: T

    open fun preSetup(project: Project) {
        project.extensions.add(EXTENSION_NAME, extension)
        project.configurations.create(CONFIGURATION_MC_DEPS)
    }

    open fun setup(project: Project) {
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
        run { // i hate a cluttered scope aaaa
            for (element in project.getVersionJson().getAsJsonArray("libraries")) {
                val obj = element.asJsonObject
                if (shouldIncludeDependency(obj)) {
                    project.dependencies.add(CONFIGURATION_MC_DEPS, getDependencyString(obj))
                }
            }
        }
    }

    override fun apply(target: Project) {
        preSetup(target)
        target.afterEvaluate {
            setup(it)
        }
    }
}