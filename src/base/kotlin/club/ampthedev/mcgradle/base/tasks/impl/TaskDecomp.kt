package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.decomp.Decompiler
import club.ampthedev.mcgradle.base.tasks.impl.decomp.old.OldDecompiler
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.PrintStream

open class TaskDecomp : BaseTask(DEOBF_JAR) {
    @InputFile
    lateinit var input: File

    @InputFiles
    lateinit var classpath: FileCollection

    @OutputFile
    lateinit var output: File

    @TaskAction
    fun decompile() {
        val tempDir = File(project.string(DECOMP_TEMP))
        prepareDirectory(tempDir)
        val tempJar = File(tempDir, input.name)

        val printStream = PrintStream(File(tempDir, "decompiler.log").absolutePath)
        val decompiler = if (project.newDecomp) {
            val clazz = Class.forName(OldDecompiler::class.java.name.replace("Old", "New").replace("old", "new"))
            val constr = clazz.getConstructor(File::class.java, PrintStream::class.java)
            constr.newInstance(tempDir, printStream) as Decompiler
        } else {
            OldDecompiler(tempDir, printStream)
        }

        if (Runtime.getRuntime().maxMemory() < 2000L * 1024 * 1024) {
            logger.warn("There might not be enough RAM allocated to Gradle to decompile MC!")
        }

        decompiler.addInput(input)

        for (file in classpath.files) {
            decompiler.addLibrary(file)
        }

        decompiler.start()

        System.gc()
        output.delete()
        tempJar.copyTo(output, overwrite = true)
    }

    override fun setup() {
        if (!::input.isInitialized) input = File(project.string(DEOBFED_JAR))
        if (!::output.isInitialized) output = File(project.string(DECOMP_JAR))
        if (!::classpath.isInitialized) classpath = project.configurations.getByName(CONFIGURATION_MC_DEPS)
    }
}