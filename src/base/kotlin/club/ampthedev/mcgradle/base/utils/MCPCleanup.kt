package club.ampthedev.mcgradle.base.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.regex.Matcher

object MCPCleanup {
    private val CLEANUP_PI5D = "0\\.6283[0-9]*[Dd]".toPattern()
    private val CLEANUP_PI5F = "0\\.6283[0-9]*[Ff]".toPattern()
    private val CLEANUP_2PI5F = "1\\.2566[0-9]*[Ff]".toPattern()
    private val CLEANUP_IFSTARTS = "(?m)(^(?![\\s{}]*$).+(?:\\r\\n|\\r|\\n))((?:[ \\t]+)if.*)".toPattern()
    private val CLEANUP_FOOTER = "\\s+$".toPattern()
    private val CLEANUP_TRAILING = "(?m)[ \\t]+$".toPattern()
    private val CLEANUP_NEWLINES = "(?m)^\\s*(?:\\r\\n|\\r|\\n){2,}".toPattern()
    private val CLEANUP_BLOCKENDS = "(?m)(?<=[;}])\\s+(?=(?:\\r\\n|\\r|\\n)\\s*})".toPattern()
    private val CLEANUP_GL = "\\s*\\/\\*\\s*GL_[^*]+\\*\\/\\s*".toPattern()
    private val CLEANUP_185PI100F = "0\\.8119[0-9]*[Ff]".toPattern()
    private val CLEANUP_UNICODE = "'\\\\u([0-9a-fA-F]{4})'".toPattern()
    private val CLEANUP_7PI100D = "0\\.21991[0-9]*[Dd]".toPattern()
    private val CLEANUP_7PI100F = "0\\.21991[0-9]*[Ff]".toPattern()
    private val CLEANUP_185PI100D = "5\\.8119[0-9]*[Dd]".toPattern()
    private val CLEANUP_2PIF = "6\\.2831[0-9]*[Ff]".toPattern()
    private val CLEANUP_PI4D = "0\\.7853[0-9]*[Dd]".toPattern()
    private val CLEANUP_MAXD = "1\\.7976[0-9]*[Ee]\\+308[Dd]".toPattern()
    private val CLEANUP_2PID = "6\\.2831[0-9]*[Dd]".toPattern()
    private val CLEANUP_PI2D = "1\\.5707[0-9]*[Dd]".toPattern()
    private val CLEANUP_PI4F = "0\\.7853[0-9]*[Ff]".toPattern()
    private val CLEANUP_PI2F = "1\\.5707[0-9]*[Ff]".toPattern()
    private val CLEANUP_180PID = "57\\.295[0-9]*[Dd]".toPattern()
    private val CLEANUP_180PIF = "57\\.295[0-9]*[Ff]".toPattern()
    private val CLEANUP_3PI2D = "4\\.7123[0-9]*[Dd]".toPattern()
    private val CLEANUP_PI10F = "0\\.3141[0-9]*[Ff]".toPattern()
    private val CLEANUP_PI10D = "0\\.3141[0-9]*[Dd]".toPattern()
    private val CLEANUP_3PI2F = "4\\.7123[0-9]*[Ff]".toPattern()
    private val CLEANUP_BLOCKSTARTS = "(?m)(?<=\\{)\\s+(?=(?:\\r\\n|\\r|\\n)[ \\t]*\\S)".toPattern()
    private val CLEANUP_2PI9F = "0\\.6981[0-9]*[Ff]".toPattern()
    private val CLEANUP_HEADER = "^\\s+".toPattern()
    private val CLEANUP_2PI5D = "1\\.2566[0-9]*[Dd]".toPattern()
    private val CLEANUP_CHARVAL = "Character\\.valueOf\\(('.')\\)".toPattern()
    private val CLEANUP_PID = "3\\.1415[0-9]*[Dd]".toPattern()
    private val CLEANUP_PIF = "3\\.1415[0-9]*[Ff]".toPattern()
    private val CLEANUP_2PI9D = "0\\.6981[0-9]*[Dd]".toPattern()
    private val PACKAGES = arrayOf(
        "GL11",
        "GL12",
        "GL13",
        "GL14",
        "GL15",
        "GL20",
        "GL21",
        "ARBMultitexture",
        "ARBOcclusionQuery",
        "ARBVertexBufferObject",
        "ARBShaderObjects"
    )
    private val JSON: List<GLConstantGroup>
    private val CALL_REGEX = "(${PACKAGES.joinToString("|")})\\.([\\w]+)\\(.+\\)".toPattern()
    private val CONSTANT_REGEX = "(?<![-.\\w])\\d+(?![.\\w])".toPattern()
    private const val ADD_AFTER = "org.lwjgl.opengl.GL11"
    private const val CHECK = "org.lwjgl.opengl."
    private const val IMPORT_CHECK = "import $CHECK"
    private const val IMPORT_REPLACE = "import $ADD_AFTER;"

    fun fixOGL(textIn: String): String {
        if (!textIn.contains(IMPORT_CHECK)) return textIn
        var text = textIn
        text = annotateConstants(text)
        for (pack in PACKAGES) {
            if (text.contains("$pack.")) {
                text = updateImports(text, CHECK + pack)
            }
        }
        return text
    }

    private fun annotateConstants(text: String): String {
        val rootMatch = CALL_REGEX.matcher(text)
        var pack: String
        var method: String
        var fullCall: String?
        val out = StringBuffer(text.length)
        var innerOut: StringBuffer

        while (rootMatch.find()) {
            fullCall = rootMatch.group()
            pack = rootMatch.group(1)
            method = rootMatch.group(2)

            val constantMatcher = CONSTANT_REGEX.matcher(fullCall ?: "")
            innerOut = StringBuffer(fullCall.length)

            while (constantMatcher.find()) {
                val constant = constantMatcher.group()
                var answer: String? = null

                for (group in JSON) {
                    if (group.functions[pack]?.contains(method) == true) {
                        for (entry in group.constants) {
                            if (entry.value.containsKey(constant)) {
                                answer = entry.key + "." + entry.value[constant]
                            }
                        }
                    }
                }

                if (answer != null) {
                    constantMatcher.appendReplacement(innerOut, Matcher.quoteReplacement(answer))
                }
            }

            constantMatcher.appendTail(innerOut)

            if (fullCall != null) {
                rootMatch.appendReplacement(out, Matcher.quoteReplacement(innerOut.toString()))
            }
        }

        rootMatch.appendTail(out)
        return out.toString()
    }

    private fun updateImports(text: String, imp: String) = if (!text.contains("import $imp;")) {
        text.replace(IMPORT_REPLACE, "$IMPORT_REPLACE${System.getProperty("line.separator")}import $imp;")
    } else {
        text
    }

    fun cleanup(textIn: String): String {
        var text = textIn
        text = CLEANUP_HEADER.matcher(text).replaceAll("")
        text = CLEANUP_FOOTER.matcher(text).replaceAll("")
        text = CLEANUP_TRAILING.matcher(text).replaceAll("")
        text = CLEANUP_NEWLINES.matcher(text).replaceAll(System.getProperty("line.separator"))
        text = CLEANUP_IFSTARTS.matcher(text).replaceAll("$1${System.getProperty("line.separator")}$2")
        text = CLEANUP_BLOCKSTARTS.matcher(text).replaceAll("")
        text = CLEANUP_BLOCKENDS.matcher(text).replaceAll("")
        text = CLEANUP_GL.matcher(text).replaceAll("")
        text = CLEANUP_MAXD.matcher(text).replaceAll("Double.MAX_VALUE")

        run {
            val matcher = CLEANUP_UNICODE.matcher(text)
            var v: Int
            val buffer = StringBuffer(text.length)

            while (matcher.find()) {
                v = matcher.group(1).toInt(16)
                if (v > 255) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement("" + v))
                }
            }
            matcher.appendTail(buffer)
            text = buffer.toString()
        }

        text = CLEANUP_CHARVAL.matcher(text).replaceAll("$1")
        text = CLEANUP_PID.matcher(text).replaceAll("Math.PI")
        text = CLEANUP_PIF.matcher(text).replaceAll("(float)Math.PI")
        text = CLEANUP_2PID.matcher(text).replaceAll("(Math.PI * 2D)")
        text = CLEANUP_2PIF.matcher(text).replaceAll("((float)Math.PI * 2F)")
        text = CLEANUP_PI2D.matcher(text).replaceAll("(Math.PI / 2D)")
        text = CLEANUP_PI2F.matcher(text).replaceAll("((float)Math.PI / 2F)")
        text = CLEANUP_3PI2D.matcher(text).replaceAll("(Math.PI * 3D / 2D)")
        text = CLEANUP_3PI2F.matcher(text).replaceAll("((float)Math.PI * 3F / 2F)")
        text = CLEANUP_PI4D.matcher(text).replaceAll("(Math.PI / 4D)")
        text = CLEANUP_PI4F.matcher(text).replaceAll("((float)Math.PI / 4F)")
        text = CLEANUP_PI5D.matcher(text).replaceAll("(Math.PI / 5D)")
        text = CLEANUP_PI5F.matcher(text).replaceAll("((float)Math.PI / 5F)")
        text = CLEANUP_180PID.matcher(text).replaceAll("(180D / Math.PI)")
        text = CLEANUP_180PIF.matcher(text).replaceAll("(180F / (float)Math.PI)")
        text = CLEANUP_2PI9D.matcher(text).replaceAll("(Math.PI * 2D / 9D)")
        text = CLEANUP_2PI9F.matcher(text).replaceAll("((float)Math.PI * 2F / 9F)")
        text = CLEANUP_PI10D.matcher(text).replaceAll("(Math.PI / 10D)")
        text = CLEANUP_PI10F.matcher(text).replaceAll("((float)Math.PI / 10F)")
        text = CLEANUP_2PI5D.matcher(text).replaceAll("(Math.PI * 2D / 5D)")
        text = CLEANUP_2PI5F.matcher(text).replaceAll("((float)Math.PI * 2F / 5F)")
        text = CLEANUP_7PI100D.matcher(text).replaceAll("(Math.PI * 7D / 100D)")
        text = CLEANUP_7PI100F.matcher(text).replaceAll("((float)Math.PI * 7F / 100F)")
        text = CLEANUP_185PI100D.matcher(text).replaceAll("(Math.PI * 185D / 100D)")
        text = CLEANUP_185PI100F.matcher(text).replaceAll("((float)Math.PI * 185F / 100F)")

        return text
    }

    init {
        val text = MCPCleanup::class.java.getResourceAsStream("/gl.json").bufferedReader().use { it.readText() }
        JSON = Gson().fromJson(text, object : TypeToken<List<GLConstantGroup>>() {

        }.type)
    }

    private data class GLConstantGroup(
        val functions: Map<String, List<String>>,
        val constants: Map<String, Map<String, String>>
    )
}