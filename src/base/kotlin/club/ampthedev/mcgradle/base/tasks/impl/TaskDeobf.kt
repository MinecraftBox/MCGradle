package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.base.utils.mcpconfig.MCPConfigUtils
import com.google.gson.Gson
import com.google.gson.JsonParser
import de.oceanlabs.mcp.mcinjector.LVTNaming
import de.oceanlabs.mcp.mcinjector.MCInjectorImpl
import net.md_5.specialsource.*
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class MCInjectorStruct {
    var innerClasses: MutableList<InnerClass>? = null

    class InnerClass internal constructor(
        val inner_class: String
    )
}

open class TaskDeobf : BaseTask(TaskType.OTHER, GENERATE_MAPPINGS) {
    @InputFile
    lateinit var fieldCsv: File

    @InputFile
    lateinit var methodCsv: File

    @InputFile
    lateinit var jar: File

    @InputFile
    lateinit var srg: File

    @InputFile
    lateinit var exceptorCfg: File

    @InputFile
    lateinit var exceptorJson: File

    @Input
    var skipExceptor = false

    @InputFiles
    var accessTransformers: FileCollection = project.files()

    @OutputFile
    lateinit var out: File

    @TaskAction
    fun deobf() {
        var tempObfJar = if (skipExceptor) {
            out
        } else {
            File(project.string(DEOBF_TEMP_JAR))
        }
        val toDeobf = if (jar == tempObfJar) {
            val file = File(temporaryDir, "todeobf.jar")
            file.delete()
            prepareDirectory(file.parentFile)
            jar.copyTo(file, overwrite = true)
            file
        } else {
            jar
        }
        if (project.newConfig) {
            val temp2 = File(temporaryDir, "deobfTemp2.jar")
            if (skipExceptor) tempObfJar = temp2

            MCPConfigUtils.runTask(
                project,
                "rename",
                tempObfJar,
                File(temporaryDir, "rename.log"),
                input = toDeobf
            )
            if (!skipExceptor) {
                MCPConfigUtils.runTask(project,
                "mcinject",
                temp2,
                File(temporaryDir, "mcinject.log"),
                input = tempObfJar)
            }
            ZipOutputStream(out.outputStream()).use { out ->
                ZipFile(temp2).use { zip ->
                    // copy all existing entries
                    zip.entries().iterator().forEach { entry ->
                        if (!entry.isDirectory) {
                            out.putNextEntry(ZipEntry(entry.name))
                            zip.getInputStream(entry).use { it.copyTo(out) }
                        }
                    }
                    // inject distmarker classes
                    ZipFile(MCPConfigUtils.getTaskJar(project, "merge")).use { zip2 ->
                        for (entryName in arrayOf("Dist", "OnlyIn", "OnlyIns")) {
                            val entryPath = "net/minecraftforge/api/distmarker/$entryName.class"
                            val entry = zip2.getEntry(entryPath)
                            entry?.let { entry1 ->
                                out.putNextEntry(ZipEntry(entryPath))
                                zip2.getInputStream(entry1)?.use { it.copyTo(out) }
                            }
                        }
                    }
                }
            }
        } else {
            toDeobf.deobfJar(tempObfJar, srg)
            if (!skipExceptor) {
                val toExcept = if (tempObfJar == out) {
                    val file = File(temporaryDir, "toexcept.jar")
                    file.delete()
                    prepareDirectory(file.parentFile)
                    tempObfJar.copyTo(file, overwrite = true)
                    file
                } else {
                    tempObfJar
                }
                toExcept.applyExceptor(out, exceptorCfg)
            }
        }
    }

    private fun File.deobfJar(out: File, srg: File) {
        val mapping = JarMapping()
        mapping.loadMappings(srg)
        val accessMap = AccessMap()
        for (transformer in accessTransformers) {
            accessMap.loadAccessTransformer(transformer)
        }
        val srgProcessor = RemapperProcessor(null, mapping, accessMap)
        val remapper = JarRemapper(srgProcessor, mapping, null)
        val input = Jar.init(this)
        val inheritanceProvider = JointProvider()
        inheritanceProvider.add(JarProvider(input))
        mapping.setFallbackInheritanceProvider(inheritanceProvider)
        remapper.remapJar(input, out)
    }

    private fun File.applyExceptor(out: File, config: File) {
        val struct = exceptorJson.loadMCIJson()
        removeUnknown(struct)
        val json = File(project.string(TRANSFORMED_EXCEPTOR_JSON))
        json.bufferedWriter().use {
            it.write(Gson().toJson(struct))
        }
        MCInjectorImpl.process(
            canonicalPath,
            out.canonicalPath,
            config.canonicalPath,
            null,
            null,
            0,
            json.canonicalPath,
            false,
            true,
            LVTNaming.LVT
        )
    }

    private fun File.removeUnknown(config: MutableMap<String, MCInjectorStruct>) {
        ZipFile(this).use { zip ->
            val iterator = config.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val className = entry.key
                if (zip.getEntry("$className.class") == null) {
                    iterator.remove()
                    continue
                }
                val struct = entry.value
                struct.innerClasses?.removeIf { zip.getEntry("${it.inner_class}.class") == null }
            }
        }
    }

    private fun File.loadMCIJson(): MutableMap<String, MCInjectorStruct> {
        val ret = linkedMapOf<String, MCInjectorStruct>()
        val obj = reader().use { JsonParser().parse(it).asJsonObject }

        for (entry in obj.entrySet()) {
            ret[entry.key] = Gson().fromJson(entry.value, MCInjectorStruct::class.java)
        }

        return ret
    }

    override fun setup() {
        if (!::fieldCsv.isInitialized) fieldCsv = File(project.string(FIELDS_CSV))
        if (!::methodCsv.isInitialized) methodCsv = File(project.string(METHODS_CSV))
        if (!::jar.isInitialized) jar = File(project.string(MERGED_JAR))
        if (!::srg.isInitialized) srg = File(project.string(NOTCH_SRG))
        if (!::exceptorCfg.isInitialized) exceptorCfg = File(project.string(SRG_EXC))
        if (!::exceptorJson.isInitialized) {
            exceptorJson = if (project.newConfig) {
                exceptorCfg
            } else {
                File(project.string(EXCEPTOR_JSON))
            }
        }
        if (!::out.isInitialized) out = File(project.string(DEOBFED_JAR))
    }
}
