package meteor

import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import meteor.UI.currentFile
import meteor.UI.currentProgress
import meteor.UI.currentUpdateFile
import meteor.UI.currentVersion
import meteor.UI.currentVersionColor
import meteor.UI.executable
import meteor.UI.launcherDIr
import meteor.UI.modulesFile
import meteor.UI.updating
import meteor.launcher.LauncherUpdate
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import kotlin.math.abs
import kotlin.system.exitProcess

object Update {
    val currentReleaseURL = URL("https://raw.githubusercontent.com/MeteorLite/Hosting/main/release/release.json")
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
                        if (!targetFile.name.endsWith("meteor-client.cfg"))
                            if (!targetFile.name.endsWith("java.security"))
                                if (!targetFile.name.endsWith("classlist"))
                                    if (!targetFile.name.endsWith("blocked.certs"))
                                        if (!targetFile.name.endsWith("tzmappings"))
                                            if (abs(targetFile.length() - file.size.toLong()) < 100) {
                                                updating = true
                                                shouldUpdate = true
                                            }
                    }
                } else {
                    updating = true
                    shouldUpdate = true
                }

                if (shouldUpdate) {
                    println("${targetFile.length()}-${file.size}")
                    println(remoteFile.toString())
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