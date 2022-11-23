package meteor.util

import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import meteor.ui.UI.currentFile
import meteor.ui.UI.currentProgress
import meteor.ui.UI.currentVersion
import meteor.ui.UI.currentVersionColor
import meteor.ui.UI.executable
import meteor.ui.UI.launcherDIr
import meteor.ui.UI.modulesFile
import meteor.ui.UI.updating
import meteor.model.LauncherUpdate
import meteor.ui.UI
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import kotlin.math.abs
import kotlin.system.exitProcess

object Update {
    val currentReleaseURL = URL("https://raw.githubusercontent.com/MeteorLite/Hosting/main/release/release.json")
    val currentUpdateFile = File(UI.launcherDIr,"currentUpdate.json")
    private val currentReleaseText = currentReleaseURL.readText(charset = Charset.forName("UTF-8"))
    val currentRelease = Gson().fromJson(currentReleaseText, LauncherUpdate::class.java)

    fun update() {
        currentUpdateFile.writeBytes(currentReleaseURL.readBytes())
        val update = Gson().fromJson(currentReleaseURL.readText(charset = Charset.forName("UTF-8")), LauncherUpdate::class.java)
        currentVersionColor = if (currentVersion == currentRelease.version) {
            Color.Green
        } else {
            Color.Red
        }
        val baseURLString = "https://github.com/MeteorLite/Hosting/raw/main/release"
        update?.let {
            var max = it.files.size
            for ((i, file) in it.files.withIndex()) {
                currentFile = file.name
                currentProgress = i.toFloat() / max.toFloat()
                val remoteFile = URL(baseURLString + file.name.replaceFirst("meteor-client\\", "/").replace("\\", "/"))
                val targetFile = File(launcherDIr.absolutePath + "/" + file.name)
                val targetParent = File(targetFile.parent)
                var shouldUpdate = false

                if (targetFile.exists()) {
                    if (abs(targetFile.length() - file.size.toLong()) > 2) {
                        if (!targetFile.name.endsWith("java.security") &&
                            !targetFile.name.endsWith("classlist") &&
                            !targetFile.name.endsWith("blocked.certs") &&
                            !targetFile.name.endsWith("tzmappings") &&
                            abs(targetFile.length() - file.size.toLong()) < 100 ) {
                            updating = true
                            shouldUpdate = true
                        }
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
            max = it.modulesParts.size
            if (!modulesFile.exists()) {
                for ((i, file) in it.modulesParts.withIndex()) {
                    currentFile = file.name
                    currentProgress = i.toFloat() / max.toFloat()
                    val remoteFile = URL(baseURLString + "/" + file.name.replace("\\", "/"))
                    val targetFile = File(launcherDIr.absolutePath + "/" + file.name)
                    val targetParent = File(targetFile.parent)
                    var shouldUpdate = false

                    if (targetFile.exists()) {
                        if (abs(targetFile.length() - file.size.toLong()) > 0) {
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
            updating = false
            currentProgress = 1f
            currentFile = ""
            Runtime.getRuntime().exec(executable)
            exitProcess(0)
        }
    }
}