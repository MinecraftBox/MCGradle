package club.ampthedev.mcgradle.user

import club.ampthedev.mcgradle.base.BaseExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class UserExtension(project: Project) : BaseExtension(project) {
    var sourceSet: SourceSet? = null

    fun sourceSet(ss: SourceSet?) {
        sourceSet = ss
    }
}