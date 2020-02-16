package club.ampthedev.mcgradle.base.tasks.impl

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import club.ampthedev.mcgradle.base.tasks.TaskType
import club.ampthedev.mcgradle.base.tasks.ZipEditTask
import club.ampthedev.mcgradle.base.utils.*
import org.gradle.api.tasks.InputFile
import java.io.File
import java.lang.StringBuilder
import java.util.*
import java.util.zip.ZipEntry

open class TaskSourceDeobf : ZipEditTask(TaskType.OTHER, APPLY_MCP_PATCHES) {
    @InputFile
    lateinit var methodCsv: File

    @InputFile
    lateinit var fieldCsv: File

    @InputFile
    lateinit var paramCsv: File

    private val methods = hashMapOf<String, String>()
    private val methodDocs = hashMapOf<String, String>()
    private val fields = hashMapOf<String, String>()
    private val fieldDocs = hashMapOf<String, String>()
    private val params = hashMapOf<String, String>()

    override fun edit(entry: ZipEntry, bytes: ByteArray): ByteArray {
        if (entry.name.endsWith(".java")) {
            val content = bytes.toString(Charsets.UTF_8)
            val lines = arrayListOf<String>()

            for (line in content.lines()) {
                var matcher = METHOD.matcher(line)
                if (matcher.find()) {
                    val javadoc = methodDocs[matcher.group(2)]
                    if (!javadoc.isNullOrEmpty()) {
                        lines.insetAboveAnnotation(buildJavadoc(matcher.group(1), javadoc, true))
                    }
                } else {
                    matcher = FIELD.matcher(line)
                    if (matcher.find()) {
                        val javadoc = fieldDocs[matcher.group(2)]
                        if (!javadoc.isNullOrEmpty()) {
                            lines.insetAboveAnnotation(buildJavadoc(matcher.group(1), javadoc, false))
                        }
                    }
                }

                val buf = StringBuffer()
                matcher = SRG_FINDER.matcher(line)
                while (matcher.find()) {
                    var find = matcher.group()
                    find = when {
                        find.startsWith("p_") -> params[find]
                        find.startsWith("func_") -> methods[find]
                        find.startsWith("field_") -> fields[find]
                        else -> find
                    }
                    if (find == null) find = matcher.group()
                    matcher.appendReplacement(buf, find)
                }
                matcher.appendTail(buf)
                lines.add(buf.toString())
            }

            return lines.joinToString("\n").toByteArray()
        }
        return bytes
    }

    override fun initTask() {
        methodCsv.loadCsv(methods, methodDocs)
        fieldCsv.loadCsv(fields, fieldDocs)
        paramCsv.loadCsv(params, null, false)
    }

    private fun buildJavadoc(indent: String, javadoc: String, isMethod: Boolean): String {
        val builder = StringBuilder()
        val list = LinkedList<String>()
        for (line in javadoc.split("\\n")) {
            list.addAll(line.wrap(120 - indent.length + 3))
        }

        if (list.size > 1 || isMethod) {
            builder.append(indent)
            builder.append("/**\n")

            for (line in list) {
                builder.append(indent)
                builder.append(" * ")
                builder.append(line)
                builder.append('\n')
            }

            builder.append(indent)
            builder.append(" */")
        } else {
            builder.append("$indent/** $javadoc */")
        }
        return builder.toString()
    }

    private fun String.wrap(len: Int): List<String> {
        if (len <= 0) return arrayListOf(this)
        if (length <= len) return arrayListOf(this)
        val lines = LinkedList<String>()
        val line = StringBuilder()
        val word = StringBuilder()
        var tempNum: Int

        for (c in toCharArray()) {
            if (c == ' ' || c == ',' || c == '-') {
                word.append(c)
                tempNum = if (c.isWhitespace()) 1 else 0

                if (line.length + word.length - tempNum > len) {
                    lines.add(line.toString())
                    line.delete(0, line.length)
                }

                word.delete(0, word.length)
            } else {
                word.append(c)
            }
        }
        if (word.isNotEmpty()) {
            if (line.length + word.length > len) {
                lines.add(line.toString())
                line.delete(0, line.length)
            }
            line.append(word)
        }
        if (line.isNotEmpty()) {
            lines.add(line.toString())
        }

        val temp = arrayListOf<String>()
        for (l in lines) {
            temp.add(l.trim())
        }
        return temp
    }

    private fun MutableList<String>.insetAboveAnnotation(line: String) {
        var back = 0
        while (this[size - 1 - back].trim().startsWith("@")) {
            back++
        }
        add(size - back, line)
    }

    private fun File.loadCsv(
        data: MutableMap<String, String>,
        docs: MutableMap<String, String>?,
        addDocs: Boolean = true
    ) {
        for (s in CSVReader(
            reader(),
            CSVParser.DEFAULT_SEPARATOR,
            CSVParser.DEFAULT_QUOTE_CHARACTER,
            CSVParser.NULL_CHARACTER,
            1,
            false
        ).use {
            it.readAll()
        }) {
            data[s[0]] = s[1]

            if (addDocs && s[3].isNotEmpty()) {
                docs?.put(s[0], s[3])
            }
        }
    }

    override fun setup() {
        if (!::methodCsv.isInitialized) methodCsv = project.mcgFile(METHODS_CSV)
        if (!::fieldCsv.isInitialized) fieldCsv = project.mcgFile(FIELDS_CSV)
        if (!::paramCsv.isInitialized) paramCsv = project.mcgFile(PARAMS_CSV)
        input = project.mcgFile(PATCHED_JAR)
        output = project.mcgFile(SOURCE_MAPPED_JAR)
    }

    companion object {
        val SRG_FINDER = "func_[0-9]+_[a-zA-Z_]+|field_[0-9]+_[a-zA-Z_]+|p_[\\w]+_\\d+_\\b".toPattern()
        val METHOD = "^((?: {3})+|\\t+)(?:[\\w\$.\\[\\]]+ )+(func_[0-9]+_[a-zA-Z_]+)\\(".toPattern()
        val FIELD = "^((?: {3})+|\\t+)(?:[\\w\$.\\[\\]]+ )+(field_[0-9]+_[a-zA-Z_]+) *[=;]".toPattern()
    }
}