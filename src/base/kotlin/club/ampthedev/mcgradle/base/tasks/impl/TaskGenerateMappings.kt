package club.ampthedev.mcgradle.base.tasks.impl

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.utils.*
import net.minecraftforge.srg2source.rangeapplier.SrgContainer
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskGenerateMappings : BaseTask() {
    @OutputDirectory
    lateinit var outputDirectory: File

    @TaskAction
    fun generate() {
        outputDirectory.deleteRecursively()
        prepareDirectory(outputDirectory)
        for (file in project.configurations.getByName(CONFIGURATION_MCP_DATA)) {
            project.zipTree(file).visit(ExtractingFileVisitor(outputDirectory))
        }
        for (file in project.configurations.getByName(CONFIGURATION_MCP_MAPS)) {
            project.zipTree(file).visit(ExtractingFileVisitor(outputDirectory))
        }

        val inSrgFile = File(project.string(JOINED_SRG))
        val inExc = File(project.string(JOINED_EXC))
        val methodsCsv = File(project.string(METHODS_CSV))
        val fieldsCsv = File(project.string(FIELDS_CSV))

        // we want Forge's naming so we're compatible with things like Mixin
        val notchToSrgFile = File(project.string(NOTCH_SRG))
        val notchToMcpFile = File(project.string(NOTCH_MCP))
        val srgToMcpFile = File(project.string(SRG_MCP))
        val mcpToSrgFile = File(project.string(MCP_SRG))
        val mcpToNotchFile = File(project.string(MCP_NOTCH))
        val srgExcFile = File(project.string(SRG_EXC))
        val mcpExcFile = File(project.string(MCP_EXC))

        val methods = hashMapOf<String, String>()
        val fields = hashMapOf<String, String>()

        CSVReader(methodsCsv.reader(),  CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER, 1, false).use {
            for (strings in it.readAll()) {
                methods[strings[0]] = strings[1]
            }
        }

        CSVReader(fieldsCsv.reader(),  CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER, 1, false).use {
            for (strings in it.readAll()) {
                fields[strings[0]] = strings[1]
            }
        }

        val inSrg = SrgContainer().readSrg(inSrgFile)
        inSrg.packageMap["club/ampthedev/mcgradle/base/annotations"] = "net/minecraftforge/fml/relauncher"
        for (f in arrayOf(notchToMcpFile, notchToSrgFile, srgToMcpFile, mcpToNotchFile, mcpToSrgFile, srgExcFile, mcpExcFile)) {
            prepareDirectory(f.parentFile)
        }

        val notchToSrg = notchToSrgFile.bufferedWriter()
        val notchToMcp = notchToMcpFile.bufferedWriter()
        val srgToMcp = srgToMcpFile.bufferedWriter()
        val mcpToSrg = mcpToSrgFile.bufferedWriter()
        val mcpToNotch = mcpToNotchFile.bufferedWriter()

        var line: String
        var temp: String
        var mcpName: String

        for (entry in inSrg.packageMap) {
            line = "PK: ${entry.key} ${entry.value}"
            notchToSrg.write(line)
            notchToSrg.newLine()

            notchToMcp.write(line)
            notchToMcp.newLine()

            mcpToNotch.write("PK: ${entry.value} ${entry.key}")
            mcpToNotch.newLine()
        }

        for (entry in inSrg.classMap) {
            line = "CL: ${entry.key} ${entry.value}"
            notchToSrg.write(line)
            notchToSrg.newLine()

            notchToMcp.write(line)
            notchToMcp.newLine()

            srgToMcp.write("CL: ${entry.value} ${entry.value}")
            srgToMcp.newLine()

            mcpToSrg.write("CL: ${entry.value} ${entry.value}")
            mcpToSrg.newLine()

            mcpToNotch.write("CL: ${entry.value} ${entry.key}")
            mcpToNotch.newLine()
        }

        // yes these two are converted from java by intellij . m y fingers hur t
        for (entry in inSrg.fieldMap) {
            line = "FD: " + entry.key + " " + entry.value
            notchToSrg.write(line)
            notchToSrg.newLine()

            temp = entry.value.substring(entry.value.lastIndexOf('/') + 1)
            mcpName = entry.value
            if (fields.containsKey(temp)) {
                mcpName = mcpName.replace(temp, fields[temp]!!)
            }

            notchToMcp.write("FD: " + entry.key + " " + mcpName)
            notchToMcp.newLine()

            srgToMcp.write("FD: " + entry.value + " " + mcpName)
            srgToMcp.newLine()

            mcpToSrg.write("FD: " + mcpName + " " + entry.value)
            mcpToSrg.newLine()

            mcpToNotch.write("FD: " + mcpName + " " + entry.key)
            mcpToNotch.newLine()
        }

        for (entry in inSrg.methodMap) {
            line = "MD: " + entry.key + " " + entry.value

            notchToSrg.write(line)
            notchToSrg.newLine()

            temp = entry.value.name.substring(entry.value.name.lastIndexOf('/') + 1)
            mcpName = entry.value.toString()
            if (methods.containsKey(temp)) {
                mcpName = mcpName.replace(temp, methods[temp]!!)
            }

            notchToMcp.write("MD: " + entry.key + " " + mcpName)
            notchToMcp.newLine()

            srgToMcp.write("MD: " + entry.value + " " + mcpName)
            srgToMcp.newLine()

            mcpToSrg.write("MD: " + mcpName + " " + entry.value)
            mcpToSrg.newLine()

            mcpToNotch.write("MD: " + mcpName + " " + entry.key)
            mcpToNotch.newLine()
        }

        for (f in arrayOf(notchToMcp, notchToSrg, srgToMcp, mcpToNotch, mcpToSrg)) {
            f.flush()
            f.close()
        }

        prepareDirectory(srgExcFile.parentFile)
        prepareDirectory(mcpExcFile.parentFile)

        val srgOut = srgExcFile.bufferedWriter()
        val mcpOut = mcpExcFile.bufferedWriter()

        val excLines = inExc.bufferedReader().use { it.readLines() }

        var split: List<String>
        for (line1 in excLines) {
            srgOut.write(line1)
            srgOut.newLine()
            split = line1.split("=")

            val sigIndex = split[0].indexOf('(')
            val dotIndex = split[0].indexOf('.')
            if (sigIndex == -1 || dotIndex == -1) {
                mcpOut.write(line1)
                mcpOut.newLine()
                continue
            }

            var name = split[0].substring(dotIndex + 1, sigIndex)
            if (methods.containsKey(name)) {
                name = methods[name]!!
            }
            mcpOut.write(split[0].substring(0, dotIndex) + "." + name + split[0].substring(sigIndex) + "=" + split[1])
            mcpOut.newLine()
        }

        srgOut.flush()
        srgOut.close()

        mcpOut.flush()
        mcpOut.close()
    }

    override fun setup() {
        outputDirectory = File(project.string(MAPPINGS_DIRECTORY))
    }
}