package club.ampthedev.mcgradle.base.tasks.impl.decomp.old

import club.ampthedev.mcgradle.base.tasks.impl.decomp.Decompiler
import org.jetbrains.java.decompiler.code.CodeConstants
import org.jetbrains.java.decompiler.main.DecompilerContext
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import org.jetbrains.java.decompiler.main.extern.IResultSaver
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory
import org.jetbrains.java.decompiler.struct.StructMethod
import org.jetbrains.java.decompiler.util.InterpreterUtil
import org.jetbrains.java.decompiler.util.JADNameProvider
import java.io.File
import java.io.PrintStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class OldDecompiler(tempDir: File, logFile: PrintStream) : Decompiler {
    private val decompiler: BaseDecompiler

    override fun addInput(file: File) {
        decompiler.addSpace(file, true)
    }

    override fun addLibrary(file: File) {
        decompiler.addSpace(file, false)
    }

    override fun start() {
        decompiler.decompileContext()
    }

    init {
        val decompileOptions = hashMapOf<String, Any>(
            IFernflowerPreferences.DECOMPILE_INNER to "1",
            IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES to "1",
            IFernflowerPreferences.ASCII_STRING_CHARACTERS to "1",
            IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH to "1",
            IFernflowerPreferences.REMOVE_SYNTHETIC to "1",
            IFernflowerPreferences.REMOVE_BRIDGE to "1",
            IFernflowerPreferences.LITERALS_AS_IS to "0",
            IFernflowerPreferences.UNIT_TEST_MODE to "0",
            IFernflowerPreferences.MAX_PROCESSING_METHOD to "0",
            DecompilerContext.RENAMER_FACTORY to AdvancedJarRenamerFactory::class.java.name
        )
        decompiler = BaseDecompiler(ByteCodeProvider(), ArtifactSaver(tempDir), decompileOptions, PrintStreamLogger(logFile))
    }
}

class AdvancedJarRenamerFactory : IVariableNamingFactory {
    override fun createFactory(p0: StructMethod) = AdvancedJarRenamer(p0)
}

class AdvancedJarRenamer(private val wrapper: StructMethod) : JADNameProvider(wrapper) {
    companion object {
        private val p = "func_(\\d+)_.*".toRegex()
    }

    override fun renameAbstractParameter(abstractParam: String, index: Int): String {
        var result = abstractParam
        if (wrapper.accessFlags and CodeConstants.ACC_ABSTRACT != 0) {
            val methodName = wrapper.name
            val found = p.find(methodName)
            if (found != null) {
                result = "p_${found.groupValues[1]}_${index}_"
            }
        }
        return result
    }
}

class ByteCodeProvider : IBytecodeProvider {
    override fun getBytecode(p0: String, p1: String?): ByteArray {
        val file = File(p0)
        if (p1 == null) {
            return InterpreterUtil.getBytes(file)
        } else {
            ZipFile(file).use {
                val entry = it.getEntry(p1) ?: error("Entry not found: $p1")
                return InterpreterUtil.getBytes(it, entry)
            }
        }
    }
}

class ArtifactSaver(private val root: File) : IResultSaver {
    private val mapArchiveStreams = hashMapOf<String, ZipOutputStream>()
    private val mapArchiveEntries = hashMapOf<String, MutableSet<String>>()

    private val String.absolutePath: String
        get() = File(root, this).absolutePath

    override fun saveFolder(p0: String) {
        val dir = File(p0.absolutePath)
        if (!(dir.mkdirs() || dir.isDirectory)) {
            error("Cannot create directory $dir")
        }
    }

    override fun copyFile(p0: String, p1: String, p2: String) {
        InterpreterUtil.copyFile(File(p0), File(p1.absolutePath, p2))
    }

    override fun saveClassFile(p0: String, p1: String, p2: String, p3: String, p4: IntArray) {
        val file = File(p0.absolutePath, p2)
        file.writer().use {
            it.write(p3)
        }
    }

    override fun createArchive(p0: String, p1: String, p2: Manifest?) {
        val file = File(p0.absolutePath, p1)
        if (!(file.createNewFile() || file.isFile)) {
            error("Cannot create file $file")
        }

        val fileStream = file.outputStream()
        val zipStream = if (p2 != null) JarOutputStream(fileStream, p2) else ZipOutputStream(fileStream)
        mapArchiveStreams[file.path] = zipStream
    }

    override fun saveDirEntry(p0: String, p1: String, p2: String) {
        saveClassEntry(p0, p1, null, p2, null)
    }

    override fun copyEntry(p0: String, p1: String, p2: String, p3: String) {
        val file = File(p1.absolutePath, p2).path

        if (!checkEntry(p3, file)) {
            return
        }

        ZipFile(File(p0)).use {
            val entry = it.getEntry(p3)
            if (entry != null) {
                val input = it.getInputStream(entry)
                val out = mapArchiveStreams[file]!!
                out.putNextEntry(ZipEntry(p3))
                InterpreterUtil.copyStream(input, out)
                input.close()
            }
        }
    }

    override fun saveClassEntry(p0: String, p1: String, p2: String?, p3: String, p4: String?) {
        val file = File(p0.absolutePath, p1).path

        if (!checkEntry(p3, file)) {
            return
        }

        val out = mapArchiveStreams[file]!!
        out.putNextEntry(ZipEntry(p3))
        if (p4 != null) {
            out.write(p4.toByteArray())
        }
    }

    private fun checkEntry(entryName: String, file: String) =
        mapArchiveEntries.computeIfAbsent(file) { hashSetOf() }.add(entryName)

    override fun closeArchive(p0: String, p1: String) {
        val file = File(p0.absolutePath, p1).path
        mapArchiveEntries.remove(file)
        mapArchiveStreams.remove(file)?.close()
    }
}