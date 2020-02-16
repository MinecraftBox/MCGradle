package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.StringBuilder
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

open class TaskRecompile : BaseTask(SOURCE_DEOBF) {
    @InputFile
    lateinit var sourceJar: File

    @InputFiles
    lateinit var classpath: FileCollection

    @OutputFile
    lateinit var outputJar: File

    @TaskAction
    fun recompile() {
        val sourceDir = File(temporaryDir, "sources")
        val classes = File(temporaryDir, "classes")
        sourceDir.deleteRecursively()
        classes.deleteRecursively()
        prepareDirectory(sourceDir)
        prepareDirectory(classes)
        project.zipTree(sourceJar).visit {
            if (it.isDirectory) return@visit
            if (it.path.startsWith("META-INF/") || it.name.startsWith("Log4j-") || it.name
                    .endsWith(".der")
            ) return@visit
            val dest = File(sourceDir, it.path)
            prepareDirectory(dest.parentFile)
            it.copyTo(dest)
        }
        updateExtDirs()
        ant.invokeMethod(
            "javac",
            mapOf(
                "srcDir" to sourceDir.canonicalPath,
                "destDir" to classes.canonicalPath,
                "failonerror" to true,
                "includeantruntime" to false,
                "classpath" to classpath.asPath,
                "encoding" to "utf-8",
                "source" to "1.8",
                "target" to "1.8",
                "debug" to true
            )
        )
        prepareDirectory(outputJar.parentFile)

        val entriesAdded = hashSetOf<String>()
        JarOutputStream(outputJar.outputStream()).use { zout ->
            val visitor = object : FileVisitor {
                var removeJava = true

                override fun visitFile(p0: FileVisitDetails) {
                    val name = p0.relativePath.toString().replace("\\", "/")
                    if (entriesAdded.contains(name) || (removeJava && name.endsWith(".java"))) {
                        return
                    }
                    entriesAdded.add(name)
                    zout.putNextEntry(ZipEntry(name))
                    p0.copyTo(zout)
                }

                override fun visitDir(p0: FileVisitDetails) {
                    var name = p0.relativePath.toString().replace("\\", "/")
                    if (!name.endsWith("/")) {
                        name += "/"
                    }
                    if (entriesAdded.contains(name)) {
                        return
                    }
                    entriesAdded.add(name)
                    val entry = ZipEntry(name)
                    zout.putNextEntry(entry)
                }
            }
            project.fileTree(sourceDir).visit(visitor)
            visitor.removeJava = false
            project.fileTree(classes).visit(visitor)
        }
    }

    private fun updateExtDirs() {
        val current = System.getProperty("java.ext.dirs")
        val new = StringBuilder()
        val parts = current.split(File.pathSeparator)
        if (parts.isNotEmpty()) {
            val lastPart = parts[parts.size - 1]
            for (part in parts) {
                if (part != "/System/Library/Java/Extensions") {
                    new.append(part)
                    if (part != lastPart) {
                        new.append(File.pathSeparator)
                    }
                }
            }
        }
        System.setProperty("java.ext.dirs", new.toString())
    }

    override fun setup() {
        if (!::sourceJar.isInitialized) sourceJar = project.mcgFile(SOURCE_MAPPED_JAR)
        if (!::outputJar.isInitialized) outputJar = project.mcgFile(VANILLA_RECOMPILED_JAR)
        if (!::classpath.isInitialized) classpath = project.configurations.getByName(CONFIGURATION_MC_DEPS)
    }
}