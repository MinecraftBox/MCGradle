package club.ampthedev.mcgradle.base.tasks.impl

import club.ampthedev.mcgradle.base.tasks.BaseTask
import club.ampthedev.mcgradle.base.tasks.BasicDownloadTask
import club.ampthedev.mcgradle.base.utils.ASSETS_DIRECTORY
import club.ampthedev.mcgradle.base.utils.DOWNLOAD_ASSET_INDEX
import club.ampthedev.mcgradle.base.utils.castTo
import club.ampthedev.mcgradle.base.utils.mcgFile
import com.google.gson.JsonParser
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskDownloadAssets : BaseTask(DOWNLOAD_ASSET_INDEX) {
    @InputFile
    lateinit var assetIndex: File

    @OutputDirectory
    lateinit var assetDir: File

    @TaskAction
    fun download() {
        val obj1 = assetIndex.bufferedReader().use { JsonParser().parse(it) }.asJsonObject
        val objectsDir = File(assetDir, "objects")
        for (entry in obj1.getAsJsonObject("objects").entrySet()) {
            val obj = entry.value.asJsonObject
            val hash = obj["hash"].asString
            val name = "${hash.substring(0, 2)}/$hash"
            BasicDownloadTask.download("http://resources.download.minecraft.net/$name", File(objectsDir, name), hash)
        }
    }

    override fun setup() {
        if (!::assetIndex.isInitialized) {
            assetIndex = castTo<TaskDownloadAssetIndex>(project.tasks.getByName(DOWNLOAD_ASSET_INDEX)).dest!!
        }
        if (!::assetDir.isInitialized) assetDir = project.mcgFile(ASSETS_DIRECTORY)
    }
}
