package tk.amplifiable.mcgradle.base.tasks

import org.gradle.api.DefaultTask
import tk.amplifiable.mcgradle.base.utils.GROUP_MAIN
import tk.amplifiable.mcgradle.base.utils.GROUP_OTHER

enum class TaskType(val groupName: String) {
    MAIN(GROUP_MAIN),
    OTHER(GROUP_OTHER)
}

abstract class BaseTask(type: TaskType = TaskType.OTHER, vararg dependencies: String) : DefaultTask() {
    init {
        group = type.groupName
        for (dep in dependencies) {
            this.dependsOn(dep)
        }
    }
}