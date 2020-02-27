package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BaseExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File

class PatchExtension(project: Project) : BaseExtension(project) {
    var patchDir = File(project.projectDir, "patches")
    var sourceSet: SourceSet? = null

    fun patchDir(pd: File) {
        patchDir = pd
    }

    fun sourceSet(ss: SourceSet?) {
        sourceSet = ss
    }
}