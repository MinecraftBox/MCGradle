package club.ampthedev.mcgradle.base.tasks.impl.maps

import net.minecraftforge.srg2source.rangeapplier.MethodData
import net.minecraftforge.srg2source.rangeapplier.SrgContainer
import org.objectweb.asm.Type
import java.io.File
import kotlin.system.exitProcess

class TsrgContainer : SrgContainer() {
    override fun readSrg(srg: File): SrgContainer {
        var currentClass: Pair<String, String>? = null
        val methodLines = hashMapOf<Pair<String, String>, MutableList<String>>()
        for (line in srg.bufferedReader().use { it.readLines() }) {
            val parts = line.trim().split(" ")
            if (line[0].isWhitespace()) {
                if (currentClass == null) {
                    error("Invalid TSRG file: $srg")
                }
                if (parts.size == 2) {
                    fieldMap["${currentClass.first}/${parts[0]}"] = "${currentClass.second}/${parts[1]}"
                } else {
                    methodLines.computeIfAbsent(currentClass) { arrayListOf() }.add(line.trim())
                }
            } else {
                classMap[parts[0]] = parts[1]
                currentClass = Pair(parts[0], parts[1])
            }
        }
        for (entry in methodLines) {
            for (line in entry.value) {
                val parts = line.split(" ")
                val notchSig = parts[1]
                val notchData = MethodData("${entry.key.first}/${parts[0]}", notchSig)
                var srgSig = "("
                for (type in Type.getArgumentTypes(notchSig)) {
                    srgSig += when (type.sort) {
                        Type.OBJECT -> "L${classMap.getOrDefault(type.internalName, type.internalName)};"
                        Type.ARRAY -> "[L${classMap.getOrDefault(type.elementType.internalName, type.elementType.internalName)};"
                        else -> type.descriptor
                    }
                }
                srgSig += ")"
                val type = Type.getReturnType(notchSig)
                srgSig += when (type.sort) {
                    Type.OBJECT -> "L${classMap.getOrDefault(type.internalName, type.internalName)};"
                    Type.ARRAY -> "[L${classMap.getOrDefault(type.elementType.internalName, type.elementType.internalName)};"
                    else -> type.descriptor
                }
                val srgData = MethodData("${entry.key.second}/${parts[2]}", srgSig)
                methodMap[notchData] = srgData
            }
        }
        return this
    }
}