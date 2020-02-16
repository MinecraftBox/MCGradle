package club.ampthedev.mcgradle.user.tasks

import club.ampthedev.mcgradle.base.tasks.ZipEditTask
import club.ampthedev.mcgradle.base.utils.mcgFile
import club.ampthedev.mcgradle.user.utils.MOD_JAR
import com.google.common.io.ByteStreams
import com.nothome.delta.GDiffPatcher
import lzma.sdk.lzma.Decoder
import lzma.streams.LzmaInputStream
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import java.io.IOException
import java.util.jar.JarInputStream
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

open class TaskApplyBinaryPatches : ZipEditTask() {
    @Input
    lateinit var patchLzmaPath: String // i.e. binpatches.lzma

    @Input
    lateinit var patchRoot: String // i.e. client

    private val patches = hashMapOf<String, ClassPatch>()

    override fun initTask() {
        patches.clear()
        ZipFile(project.mcgFile(MOD_JAR)).use {
            val entry = it.getEntry(patchLzmaPath)
            val matcher = "binpatch/$patchRoot/.*.binpatch".toPattern()
            LzmaInputStream(it.getInputStream(entry), Decoder()).use { decompressed ->
                JarInputStream(decompressed).use { jis ->
                    while (true) {
                        val jEntry = try {
                            jis.nextJarEntry ?: break
                        } catch (e: IOException) { break }
                        if (matcher.matcher(jEntry.name).matches()) {
                            val cp = jis.readPatch()
                            patches[cp.sourceClassName.replace('.', '/') + ".class"] = cp
                        }
                    }
                }
            }
        }
    }

    override fun ZipEntry.shouldInclude() = !name.startsWith("META-INF/")

    override fun edit(entry: ZipEntry, bytes: ByteArray): ByteArray {
        val patch = patches[entry.name]
        if (patch != null) {
            val checksum = bytes.adlerHash()
            if (checksum != patch.inputChecksum) {
                throw GradleException("Failed to patch ${entry.name}")
            }
            return synchronized(patcher) {
                patcher.patch(bytes, patch.patch)
            }
        }
        return bytes
    }

    private fun JarInputStream.readPatch(): ClassPatch {
        val input = ByteStreams.newDataInput(use { readBytes() })
        val name = input.readUTF()
        val sourceClassName = input.readUTF()
        val targetClassName = input.readUTF()
        val exists = input.readBoolean()
        var inputChecksum = -1L
        if (exists) {
            inputChecksum = input.readLong()
        }
        val patchSize = input.readInt()
        val patchBytes = ByteArray(patchSize)
        input.readFully(patchBytes)
        return ClassPatch(name, sourceClassName, targetClassName, exists, patchBytes, inputChecksum)
    }

    private fun ByteArray.adlerHash(): Long {
        val hasher = Adler32()
        hasher.update(this)
        return hasher.value
    }

    companion object {
        val patcher = GDiffPatcher()
    }

    private data class ClassPatch(
        val name: String,
        val sourceClassName: String,
        val targetClassName: String,
        val existsAtTarget: Boolean,
        val patch: ByteArray,
        val inputChecksum: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ClassPatch

            if (name != other.name) return false
            if (sourceClassName != other.sourceClassName) return false
            if (targetClassName != other.targetClassName) return false
            if (existsAtTarget != other.existsAtTarget) return false
            if (!patch.contentEquals(other.patch)) return false
            if (inputChecksum != other.inputChecksum) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + sourceClassName.hashCode()
            result = 31 * result + targetClassName.hashCode()
            result = 31 * result + existsAtTarget.hashCode()
            result = 31 * result + patch.contentHashCode()
            result = 31 * result + inputChecksum.hashCode()
            return result
        }
    }

}