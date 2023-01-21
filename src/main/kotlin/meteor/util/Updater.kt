package meteor.util

import com.google.gson.Gson
import meteor.model.LauncherUpdate
import meteor.ui.UI.clientExecutable
import meteor.ui.UI.status
import meteor.ui.UI.currentProgress
import meteor.ui.UI.launcherDIr
import meteor.ui.UI.updating
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess


object Updater {
    val hostingURLString = "https://github.com/MeteorLite/Hosting/raw/main"
    val baseURLString = "$hostingURLString/release"
    val currentReleaseURL = URL("$baseURLString/release.json")
    val currentRuntimeVersion = URL("$baseURLString/runtime.version").readText()
    val currentUpdateFile = File(launcherDIr,"currentUpdate.json")
    val currentRuntimeFile = File(launcherDIr,"runtime-$currentRuntimeVersion.zip")
    val localRuntimeDir = File(launcherDIr,"runtime")
    private val currentReleaseText = currentReleaseURL.readText()
    val currentRelease = Gson().fromJson(currentReleaseText, LauncherUpdate::class.java)
    var freshInstall = false

    fun generateCRC(file: File): String {
        val crc32 = CRC32()
        val bytes = file.readBytes()
        crc32.update(bytes)
        return crc32.value.toString()
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
                        "\\app\\client.cfg" -> {
                            if (localFile.readText() != remoteFile.readText())
                                filesToUpdate.add(file)
                        }
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
            status = file.name
            currentProgress = i.toFloat() / max.toFloat()
            val remoteFile = URL(baseURLString + file.name.replaceFirst("meteor-client\\", "/").replace("\\", "/"))
            val targetFile = File(launcherDIr.absolutePath + "/" + file.name)
            val targetParent = File(targetFile.parent)

            targetParent.mkdirs()
            targetFile.delete()
            targetFile.writeBytes(remoteFile.readBytes())
            println("Updating: $remoteFile -> ${targetFile.absolutePath}")
        }
    }

    fun updateRuntime() {
        val runtimeURL = URL("$hostingURLString/runtime-$currentRuntimeVersion.zip")
        val runtimeSize = runtimeURL.openConnection().contentLength

        var needsUpdate = false

        val runtimeSizeMismatch = currentRuntimeFile.length().toInt() != runtimeSize

        if (!currentRuntimeFile.exists() || !localRuntimeDir.exists() || runtimeSizeMismatch) {
            needsUpdate = true
        }

        if (needsUpdate) {
            if (runtimeSizeMismatch) {
                status = "Downloading JDK runtime $currentRuntimeVersion... This may take a while."
                currentRuntimeFile.writeBytes(runtimeURL.readBytes())
            }
        }

        status = "Checking/Updating JDK runtime $currentRuntimeVersion..."
        unzipRuntime()
    }

    fun unzipRuntime() {
        val buffer = ByteArray(1024)
        val zis = ZipInputStream(FileInputStream(currentRuntimeFile))
        var zipEntry: ZipEntry? = zis.nextEntry
        while (zipEntry != null) {
            val newFile: File = newFile(launcherDIr, zipEntry)
            if (zipEntry.isDirectory) {
                if (!newFile.isDirectory && !newFile.mkdirs()) {
                    throw IOException("Failed to create directory $newFile")
                }
            } else {
                val parent = newFile.parentFile
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw IOException("Failed to create directory $parent")
                }

                if (zipEntry.size != newFile.length()) {
                    val fos = FileOutputStream(newFile)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
            }
            zipEntry = zis.nextEntry
        }

        zis.closeEntry()
        zis.close()
    }

    @Throws(IOException::class)
    fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }

    fun update() {
        currentUpdateFile.writeBytes(currentReleaseURL.readBytes())
        val update = Gson().fromJson(currentReleaseURL.readText(charset = Charset.forName("UTF-8")), LauncherUpdate::class.java)
        update?.let {
            updateRuntime()
            updateFiles(it)
            updating = false
            currentProgress = 1f
            status = ""
            Runtime.getRuntime().exec(clientExecutable)
            exitProcess(0)
        }
    }
}