package club.ampthedev.mcgradle.base.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import java.io.File

class DownloadTask(@Input override val url: String, @OutputFile override val dest: File, @Input override val sha1: String? = null) : BasicDownloadTask()