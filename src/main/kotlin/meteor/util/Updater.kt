package meteor.util

import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import meteor.ui.UI.currentFile
import meteor.ui.UI.currentProgress
import meteor.ui.UI.currentVersion
import meteor.ui.UI.currentVersionColor
import meteor.ui.UI.clientExecutable
import meteor.ui.UI.launcherDIr
import meteor.ui.UI.modulesFile
import meteor.ui.UI.updating
import meteor.model.LauncherUpdate
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.CRC32
import kotlin.system.exitProcess

object Updater {
    val baseURLString = "https://github.com/MeteorLite/Hosting/raw/main/release"
    val currentReleaseURL = URL("$baseURLString/release.json")
    val currentUpdateFile = File(launcherDIr,"currentUpdate.json")
    private val currentReleaseText = currentReleaseURL.readText()
    val currentRelease = Gson().fromJson(currentReleaseText, LauncherUpdate::class.java)
    var freshInstall = false
    val filesToUpdate = currentRelease.files.size
    var updatedFiles = 0
    fun generateCRC(file: File): String {
        val crc32 = CRC32()
        val bytes = file.readBytes()
        crc32.update(bytes)
        return crc32.value.toString()
    }

    fun updateFiles(it: LauncherUpdate) {
        val max = it.files.size
        for ((i, file) in it.files.withIndex()) {
            currentFile = file.name
            currentProgress = i.toFloat() / max.toFloat()
            val remoteFile = URL(baseURLString + file.name.replaceFirst("meteor-client\\", "/").replace("\\", "/"))
            val targetFile = File(launcherDIr.absolutePath + "/" + file.name)
            val targetParent = File(targetFile.parent)
            var shouldUpdate = false

            if (targetFile.exists()) {
                if (file.hash != generateCRC(targetFile)) {
                    updating = true
                    shouldUpdate = true
                }
            } else {
                updating = true
                shouldUpdate = true
            }

            if (shouldUpdate) {
                targetParent.mkdirs()
                targetFile.writeBytes(remoteFile.readBytes())
            }
        }
    }

    fun updateModules(it: LauncherUpdate) {
        val max = it.modulesParts.size
        if (!modulesFile.exists()) {
            for ((i, file) in it.modulesParts.withIndex()) {
                currentFile = file.name
                currentProgress = i.toFloat() / max.toFloat()
                val remoteFile = URL(baseURLString + "/" + file.name.replace("\\", "/"))
                val targetFile = File(launcherDIr.absolutePath + "/" + file.name)
                val targetParent = File(targetFile.parent)
                var shouldUpdate = false

                if (targetFile.exists()) {
                    if (file.hash != generateCRC(targetFile)) {
                        shouldUpdate = true
                    }
                } else shouldUpdate = true

                if (shouldUpdate) {
                    println("${targetFile.length()}-${file.size}")
                    println(remoteFile.toString())
                    targetParent.mkdirs()
                    targetFile.writeBytes(remoteFile.readBytes())
                }
            }
            Zipper.zipModules()
        }
    }

    fun update() {
        currentUpdateFile.writeBytes(currentReleaseURL.readBytes())
        val update = Gson().fromJson(currentReleaseURL.readText(charset = Charset.forName("UTF-8")), LauncherUpdate::class.java)
        currentVersionColor = if (currentVersion.value == currentRelease.version) {
            if (updatedFiles >= filesToUpdate) {
                Color.Green
            } else {
                Color.Yellow
            }
        } else {
            Color.Red
        }
        update?.let {
            updateFiles(it)
            updateModules(it)
            updating = false
            currentProgress = 1f
            currentFile = ""
            Runtime.getRuntime().exec(clientExecutable)
            exitProcess(0)
        }
    }
}