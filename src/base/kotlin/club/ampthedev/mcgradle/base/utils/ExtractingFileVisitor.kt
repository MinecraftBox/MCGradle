package club.ampthedev.mcgradle.base.utils

import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import java.io.File

class ExtractingFileVisitor(private val out: File) : FileVisitor {
    override fun visitFile(p0: FileVisitDetails) {
        val outFile = File(out, p0.path)
        prepareDirectory(outFile.parentFile)
        p0.copyTo(outFile)
    }

    override fun visitDir(p0: FileVisitDetails) {
        prepareDirectory(File(out, p0.path))
    }
}