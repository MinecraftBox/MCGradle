package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.ZipEditTask
import club.ampthedev.mcgradle.base.utils.ContextualPatch
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import java.io.File
import java.util.zip.ZipEntry

open class TaskApplyPatches : ZipEditTask() {
    @InputDirectory
    lateinit var patches: File
    @Input
    var mcpPatch = false

    private val loadedPatches = hashMapOf<String, File>()

    override fun initTask() {
        loadedPatches.clear()
        project.fileTree(patches).visit {
            if (!it.isDirectory) {
                if (!it.name.endsWith(".patch")) return@visit
                val patchFile: File = it.file
                var fileName = patchFile.path
                fileName = fileName.substring(patches.path.length + 1)
                fileName = fileName.substring(0, fileName.length - ".java.patch".length)
                fileName = fileName.replace('.', '/').replace(File.separatorChar, '/')
                fileName += ".java"
                loadedPatches[fileName] = patchFile
            }
        }
    }

    override fun edit(entry: ZipEntry, bytes: ByteArray): ByteArray {
        if (entry.name.endsWith(".java")) {
            var content = bytes.toString(Charsets.UTF_8)
            if (mcpPatch) {
                content = content.replace(TRAILING, "")
                content = content.replace(NEWLINES, System.getProperty("line.separator"))
            }
            val patchFile = loadedPatches[entry.name]
            if (patchFile != null) {
                val provider = ContextProvider(content)
                val patch = ContextualPatch.create(patchFile.bufferedReader().use { it.readText() }, provider)
                    .setAccessC14N(true)
                val errors = patch.patch(false)

                var error: Throwable? = null
                for (rep in errors) {
                    if (!rep.status.isSuccess) {
                        logger.error("Patching failed: ${rep.target}")
                        for (hunk in rep.hunks) {
                            if (!hunk.status.isSuccess) {
                                logger.error("Hunk ${hunk.hunkID} failed! ${rep.failure.message}")
                                logger.error(hunk.hunk.lines.joinToString("\n"))
                            }
                        }
                        error = rep.failure
                    }
                }
                if (error != null) {
                    throw error
                }
                content = provider.getAsString()
            }
            return content.toByteArray()
        }
        return bytes
    }

    private class ContextProvider(file: String) : ContextualPatch.IContextProvider {
        private var d = file.lines()

        override fun getData(target: String?): MutableList<String> {
            val out = ArrayList<String>(d.size + 5)
            out.addAll(d)
            return out
        }

        override fun setData(target: String?, data: MutableList<String>) {
            d = data
        }

        fun getAsString() = d.joinToString("\n")
    }

    companion object {
        private val TRAILING = "(?m)[ \\t]+\$".toRegex()
        private val NEWLINES = "(?m)^(\\r\\n|\\r|\\n){2,}".toRegex()
    }
}