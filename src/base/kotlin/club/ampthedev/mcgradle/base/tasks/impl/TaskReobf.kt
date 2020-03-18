package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.ClassLoaderProvider
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.net.URL
import java.net.URLClassLoader

open class TaskReobf : BaseTask(GENERATE_MAPPINGS) {
    @InputFile
    lateinit var input: File

    @InputFile
    lateinit var deobfJar: File

    @InputFile
    lateinit var srg: File

    @InputFile
    lateinit var methodsCsv: File

    @InputFile
    lateinit var fieldsCsv: File

    @InputFiles
    lateinit var libs: FileCollection

    @OutputFile
    lateinit var output: File

    override fun setup() {
        if (!::deobfJar.isInitialized) deobfJar = project.mcgFile(DEOBFED_JAR)
        if (!::srg.isInitialized) srg = project.mcgFile(MCP_NOTCH)
        if (!::methodsCsv.isInitialized) methodsCsv = project.mcgFile(METHODS_CSV)
        if (!::fieldsCsv.isInitialized) fieldsCsv = project.mcgFile(FIELDS_CSV)
        if (!::libs.isInitialized) libs = project.configurations.getByName(CONFIGURATION_MC_DEPS)
    }

    @TaskAction
    fun reobfuscate() {
        /*val exceptor = ReobfExceptor()
        exceptor.toReobfJar = input
        exceptor.deobfJar = deobfJar
        exceptor.fieldCSV = fieldsCsv
        exceptor.methodCSV = methodsCsv
        val out = File(temporaryDir, "class_reobf.srg")
        exceptor.doFirstThings()
        exceptor.buildSrg(srg, out)*/
        val mapping = JarMapping()
        mapping.loadMappings(srg.bufferedReader(), null, null, false)
        val remapper = JarRemapper(null, mapping)
        val input = Jar.init(input)
        val inheritanceProviders = JointProvider()
        inheritanceProviders.add(JarProvider(input))
        inheritanceProviders.add(ClassLoaderProvider(URLClassLoader(libs.toUrlArray())))
        mapping.setFallbackInheritanceProvider(inheritanceProviders)
        prepareDirectory(output.parentFile)
        remapper.remapJar(input, output)
    }

    private fun FileCollection.toUrlArray(): Array<URL> {
        val files = asFileTree.files
        val list = arrayListOf<URL>()
        for (file in files) {
            list.add(file.toURI().toURL())
        }
        return list.toTypedArray()
    }
}