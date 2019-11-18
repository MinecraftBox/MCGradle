package tk.amplifiable.mcgradle.base.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import tk.amplifiable.mcgradle.base.tasks.BaseTask
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.KClass

// version manifest data
data class Version(val id: String, val type: String, val url: String) // there's also time and releaseTime fields, but we don't need them
data class VersionManifest(val versions: Array<Version>) // there's also latest version data, but we don't need that
{
    // BEFORE YOU YELL AT ME intellij said i should do this
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionManifest

        if (!versions.contentEquals(other.versions)) return false

        return true
    }

    override fun hashCode(): Int {
        return versions.contentHashCode()
    }
}

val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
private val replacements = hashMapOf<Project, HashMap<String, String>>()
private val validPropertyClasses = arrayListOf<Class<*>>(
        String::class.java
)
private lateinit var versionManifestObj: VersionManifest
private lateinit var versionJsonObj: JsonObject

fun Project.getVersionManifest(): VersionManifest {
    if (!::versionManifestObj.isInitialized) {
        // load version manifest
        val versionManifestFile = mcgFile(VERSION_MANIFEST_LOCATION)
        val versionManifestContent = try {
            val result = with (url(VERSION_MANIFEST_URL).openConnection() as HttpURLConnection) {
                addRequestProperty("User-Agent", USER_AGENT)
                inputStream.bufferedReader().use { it.readText() }
            }
            prepareDirectory(versionManifestFile.parentFile)
            versionManifestFile.bufferedWriter().use { it.write(result) }
            result
        } catch (e: Exception) {
            if (versionManifestFile.exists()) {
                versionManifestFile.bufferedReader().use { it.readText() }
            } else {
                throw e
            }
        }
        versionManifestObj = GSON.fromJson(versionManifestContent, VersionManifest::class.java)
    }
    return versionManifestObj
}

fun getVersionData(manifest: VersionManifest, id: String): Version {
    for (version in manifest.versions) {
        if (version.id == id) {
            return version
        }
    }
    throw IllegalArgumentException("Unknown version $id")
}

fun Project.getVersionJson(): JsonObject {
    if (!::versionJsonObj.isInitialized) {
        // load version json
        val versionJsonFile = mcgFile(VERSION_DATA_LOCATION)
        val versionJsonContent = if (versionJsonFile.exists()) {
            versionJsonFile.bufferedReader().use { it.readText() }
        } else {
            val result = with (url(getVersionData(getVersionManifest(), string(MC_VERSION)).url).openConnection() as HttpURLConnection) {
                addRequestProperty("User-Agent", USER_AGENT)
                inputStream.bufferedReader().use { it.readText() }
            }
            prepareDirectory(versionJsonFile.parentFile)
            versionJsonFile.bufferedWriter().use { it.write(result) }
            result
        }
        versionJsonObj = JsonParser.parseString(versionJsonContent).asJsonObject
    }
    return versionJsonObj
}

fun Project.addReplacement(name: String, value: String) {
    if (name[0] != '@' || name[name.length - 1] != '@') throw IllegalArgumentException("Invalid replacement name $name")
    replacements.computeIfAbsent(this) {
        hashMapOf()
    }[name] = value
}

fun Project.url(url: String) = URL(string(url))

fun Project.mcgFile(str: String): File = file(string(str))

fun Project.addReplacements(replacements: Map<String, String>) {
    for (replacement in replacements) {
        addReplacement(replacement.key, replacement.value)
    }
}

fun Project.string(str: String, vararg args: Any?): String {
    val r = replacements[this]
    var result = str
    if (r != null) {
        for (entry in r) {
            result = result.replace(entry.key, entry.value)
        }
    }
    return String.format(result, args)
}

fun prepareDirectory(dir: File?) {
    if (dir != null && !dir.exists() && !dir.mkdirs()) {
        throw IOException("Couldn't create directory $dir")
    }
}

fun Project.task(name: String, task: Class<out BaseTask>): Task = tasks.create(name, task)

fun Project.task(name: String, task: KClass<out BaseTask>) = task(name, task.java)

fun checkValidConstantProperty(it: Any?) {
    if (it == null) throw GradleException("Properties cannot be null")
    if (!validPropertyClasses.contains(it.javaClass)) throw GradleException("Invalid property type ${it.javaClass.name}")
}

@Suppress("UNCHECKED_CAST")
fun <T> castTo(obj: Any) = obj as T // i hate unchecked warnings so this method is perfect for me - amp

fun <T : Task> Project.getTask(name: String) = castTo<T>(project.tasks.getByName(name))