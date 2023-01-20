package meteor.util

import com.google.gson.Gson
import meteor.ui.UI.currentFile
import meteor.ui.UI.currentProgress
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

    fun generateCRC(file: File): String {
        val crc32 = CRC32()
        val bytes = file.readBytes()
        crc32.update(bytes)
        return crc32.value.toString()
    }

    fun checkStrangeFile(fileData: meteor.model.File, localFile: File, remoteFile: URL, filesToUpdate: ArrayList<meteor.model.File>) {
        if (localFile.readText() != remoteFile.readText())
            filesToUpdate.add(fileData)
    }

    fun getUpdateList(it: LauncherUpdate) : ArrayList<meteor.model.File> {
        val filesToUpdate = ArrayList<meteor.model.File>()
        for (file in it.files) {
            val localFile = File(launcherDIr.absolutePath + "/" + file.name)
            if (localFile.exists()) {
                val crc = generateCRC(localFile)
                if (file.hash != crc) {
                    val remoteFile = URL(baseURLString + file.name.replaceFirst("meteor-client\\", "/").replace("\\", "/"))
                    when (file.name) {
                        "\\app\\client.cfg",
                        "\\runtime\\conf\\security\\java.security",
                        "\\runtime\\lib\\classlist",
                        "\\runtime\\lib\\security\\blocked.certs",
                        "\\runtime\\lib\\tzmappings",
                        "\\runtime\\release"-> checkStrangeFile(file, localFile, remoteFile, filesToUpdate)
                        else -> {
                            println("file: ${file.name} CRC: $crc Expected: ${file.hash}")
                            filesToUpdate.add(file)
                        }
                    }
                }
            } else {
                filesToUpdate.add(file)
            }
        }
        return filesToUpdate
    }

    fun updateFiles(it: LauncherUpdate) {
        val filesToUpdate = getUpdateList(it)

        val max = filesToUpdate.size
        for ((i, file) in filesToUpdate.withIndex()) {
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
                targetFile.delete()
                targetFile.writeBytes(remoteFile.readBytes())
                println("Updating: $remoteFile -> ${targetFile.absolutePath}")
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