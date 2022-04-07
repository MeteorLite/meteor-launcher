package meteor

import meteor.UI.modulesFile
import java.io.File

object Zipper {
    fun zipModules() {
        val modules0File = File(System.getProperty("user.home") + "/.meteor/launcher/modules/modules-0")
        val modules1File = File(System.getProperty("user.home") + "/.meteor/launcher/modules/modules-1")
        val modules2File = File(System.getProperty("user.home") + "/.meteor/launcher/modules/modules-2")

        val modulesBytes = ArrayList<Byte>()

        modulesBytes.addAll(modules0File.readBytes().toList())
        modulesBytes.addAll(modules1File.readBytes().toList())
        modulesBytes.addAll(modules2File.readBytes().toList())

        modulesFile.writeBytes(modulesBytes.toByteArray())
    }
}