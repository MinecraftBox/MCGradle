package club.ampthedev.mcgradle.patch.tasks

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.impl.TaskDeobf
import club.ampthedev.mcgradle.base.utils.*
import club.ampthedev.mcgradle.patch.utils.GENERATE_PATCHES
import club.ampthedev.mcgradle.patch.utils.REOBFUSCATE_JAR
import com.google.common.collect.ArrayListMultimap
import com.google.common.io.ByteStreams
import com.nothome.delta.Delta
import lzma.streams.LzmaOutputStream
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.Adler32

open class TaskGenerateBinPatches :
    BaseTask(DOWNLOAD_CLIENT, GENERATE_PATCHES, DOWNLOAD_SERVER, RECOMPILE_CLEAN_TASK, REOBFUSCATE_JAR, GENERATE_MAPPINGS, "jar") {
    @InputFile
    lateinit var cleanClient: File

    @InputFile
    lateinit var cleanServer: File

    @InputFile
    lateinit var cleanDev: File

    @InputFile
    lateinit var dirtyJar: File

    @InputFile
    lateinit var dirtyDev: File

    @InputFile
    lateinit var srg: File

    @OutputFile
    lateinit var runBinPatches: File

    @OutputFile
    lateinit var devBinPatches: File

    private val patchSets = arrayListOf<File>()
    private val obfMapping = hashMapOf<String, String>()
    private val srgMapping = hashMapOf<String, String>()
    private val innerClasses = ArrayListMultimap.create<String, String>()
    private val patchedFiles = hashSetOf<String>()
    private val delta = Delta()

    @TaskAction
    fun generate() {
        loadMappings()

        for (f in patchSets) {
            val base = f.absolutePath
            for (patch in project.fileTree(f)) {
                val path = patch.absolutePath.replace(".java.patch", "")
                    .substring(base.length + 1)
                    .replace('\\', '/')
                    .replace('.', '/')
                val obfName = srgMapping[path] ?: path
                patchedFiles.add(obfName)
            }
        }

        val runtime = hashMapOf<String, ByteArray>()
        val devtime = hashMapOf<String, ByteArray>()

        runtime.createBinPatches("client/", cleanClient, dirtyJar, true)
        runtime.createBinPatches("server/", cleanServer, dirtyJar, true)
        devtime.createBinPatches("merged/", cleanDev, dirtyDev, false)

        prepareDirectory(runBinPatches.parentFile)
        prepareDirectory(devBinPatches.parentFile)

        runBinPatches.outputStream().use {
            it.write(runtime.createPatchJar().compress())
        }

        devBinPatches.outputStream().use {
            it.write(devtime.createPatchJar().compress())
        }
    }

    private fun ByteArray.compress(): ByteArray {
        val out = ByteArrayOutputStream()
        LzmaOutputStream.Builder(out).useEndMarkerMode(true).build().use {
            it.write(this)
        }
        return out.toByteArray()
    }

    private fun Map<String, ByteArray>.createPatchJar(): ByteArray {
        val out = ByteArrayOutputStream()
        JarOutputStream(out).use {
            for (entry in this) {
                it.putNextEntry(JarEntry("binpatch/${entry.key}"))
                it.write(entry.value)
            }
        }
        return out.toByteArray()
    }

    private fun MutableMap<String, ByteArray>.createBinPatches(root: String, base: File, target: File, reobf: Boolean) {
        JarFile(base).use { cleanJar ->
            JarFile(target).use { dirtyJar ->
                for (entry in obfMapping) {
                    val obf = entry.key
                    val srg = entry.value
                    if (!patchedFiles.contains(obf)) continue
                    val entryName = if (reobf) obf else srg
                    val cleanEntry = cleanJar.getJarEntry("$entryName.class") ?: continue
                    val dirtyEntry = dirtyJar.getJarEntry("$entryName.class") ?: continue
                    val clean = cleanJar.getInputStream(cleanEntry).use { it.readBytes() }
                    val dirty = dirtyJar.getInputStream(dirtyEntry).use { it.readBytes() }

                    val diff = delta.compute(clean, dirty)
                    val out = ByteStreams.newDataOutput()
                    out.writeUTF(entryName)
                    out.writeUTF(entryName.replace('/', '.'))
                    out.writeUTF(srg.replace('/', '.'))
                    out.writeBoolean(true)
                    out.writeLong(clean.adlerHash())
                    out.writeInt(diff.size)
                    out.write(diff)
                    this[root + srg.replace('/', '.') + ".binpatch"] = out.toByteArray()
                }
            }
        }
    }

    private fun ByteArray.adlerHash(): Long {
        val hasher = Adler32()
        hasher.update(this)
        return hasher.value
    }

    private fun loadMappings() {
        for (line in srg.bufferedReader().use { it.readLines() }) {
            if (!line.startsWith("CL:")) continue
            val parts = line.split(" ")
            obfMapping[parts[1].trim()] = parts[2].trim()
            val srgName = parts[2].trim()
            srgMapping[srgName] = parts[1].trim()
            val inner = srgName.indexOf('$')
            if (inner > 0) {
                val outer = srgName.substring(0, inner)
                innerClasses.put(outer, srgName)
            }
        }
    }

    override fun setup() {
        patchSets.add(project.mcgFile("$PROJECT_DIR/patches"))
        if (!::cleanClient.isInitialized) cleanClient = project.mcgFile(CLIENT_JAR)
        if (!::cleanServer.isInitialized) cleanServer = project.mcgFile(SERVER_JAR)
        if (!::cleanDev.isInitialized) cleanDev = project.mcgFile(VANILLA_RECOMPILED_JAR)
        if (!::dirtyJar.isInitialized) dirtyJar = (project.tasks.getByName(REOBFUSCATE_JAR) as TaskDeobf).out
        if (!::dirtyDev.isInitialized) dirtyDev = (project.tasks.getByName("jar") as Jar).archiveFile.get().asFile
        if (!::srg.isInitialized) srg = project.mcgFile(NOTCH_SRG)
        if (!::runBinPatches.isInitialized) runBinPatches = File(temporaryDir, "binpatches.lzma")
        if (!::devBinPatches.isInitialized) devBinPatches = File(temporaryDir, "binpatches.dev.lzma")
    }
}