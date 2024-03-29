package meteor.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.delay
import meteor.util.Updater
import meteor.util.Updater.currentUpdateFile
import meteor.model.LauncherUpdate
import java.io.File

object UI {
    val launcherDIr = File(System.getProperty("user.home") + "/.meteor/launcher/")
    val modulesFile = File(System.getProperty("user.home") + "/.meteor/launcher/runtime/lib/modules")
    val clientExecutable = "cmd /c start \"\" " + "\"" + System.getProperty("user.home") + "\\.meteor\\launcher\\client.bat" + "\""

    var requiresUpdate = false
    var currentVersion = mutableStateOf("")
    var currentVersionColor = Color.Cyan
    var status = "Checking for updates..."
    var currentProgress = 0f
    var startedThread = false
    var updating = false


    @Composable
    fun brandBadge(constraints: BoxWithConstraintsScope) {
        val bitmap: ImageBitmap = useResource("Meteor.ico") { loadImageBitmap(it) }
        Box(modifier = Modifier.width(100.dp).offset(y = constraints.maxHeight - 120.dp).background(darkThemeColors.background), contentAlignment = Alignment.TopEnd) {
            Image( bitmap = bitmap, contentDescription = "Brand Badge", filterQuality = FilterQuality.High)
        }
    }

    val darkThemeColors = darkColors(
        primary = Color.Cyan,
        primaryVariant = Color(0xFF3E2723),
        secondary = Color.Cyan,
        background = Color(0xFF242424),
        surface = Color.Black,
        error = Color.Red,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black
    )

    @Composable
    @Preview
    fun launcher() {
        if (!launcherDIr.exists())
            Updater.freshInstall = true
        if (!launcherDIr.exists()) {
            launcherDIr.mkdirs()
            requiresUpdate = true
        }

        if (!currentUpdateFile.exists())
            requiresUpdate = true

        if (!Updater.freshInstall) {
            try {
                val localUpdate = Gson().fromJson(currentUpdateFile.readText(), LauncherUpdate::class.java)
                currentVersion.value = localUpdate.version
            } catch (e: Exception) {
                currentVersion.value = "broken"
                requiresUpdate = true
            }
        }
        else {
            currentVersion.value = "missing"
            currentUpdateFile.writeBytes(Updater.currentReleaseURL.readBytes())
        }

        val brand by remember { mutableStateOf("Meteor launcher") }
        var file by remember { mutableStateOf(status) }
        var progress by remember { mutableStateOf(currentProgress) }
        var version by remember { mutableStateOf(currentVersion.value) }
        var color by remember { mutableStateOf(currentVersionColor) }
        val textMod = Modifier.width(200.dp).height(40.dp).background(darkThemeColors.background)
        LaunchedEffect(Unit) {
            for (i in 1..Int.MAX_VALUE) {
                delay(100) // update once a second
                file = status
                progress = currentProgress
                version = currentVersion.value
                if (updating)
                    currentVersionColor = Color.Yellow
                color = currentVersionColor
            }
        }
        BoxWithConstraints(modifier = Modifier.width(500.dp).height(130.dp).background(Color.Transparent).clip(
            RoundedCornerShape(size = 20.dp)
        )) {
            val constraints = this
            Box(modifier = Modifier.fillMaxSize().background(darkThemeColors.background)) {
                //Current
                Box(modifier = textMod.offset(x = 110.dp, y = 45.dp)) {
                    Text(text = "Current version: $version", color = color, fontSize = 14.sp, fontFamily = Font.robotoFont)
                }
                //Latest
                Box(modifier = textMod.offset(x = 110.dp, y = 60.dp)) {
                    Text(text = "Latest:   " + Updater.currentRelease.version, color = Color.Cyan, fontSize = 14.sp, fontFamily = Font.robotoFont)
                }
                //Meteor
                Box(modifier = textMod.offset(x = 110.dp, y = 5.dp)) {
                    Text(text = brand, color = Color.Cyan, fontSize = 32.sp, fontFamily = Font.robotoFont)
                }
                LinearProgressIndicator(progress = progress, modifier = Modifier.align(Alignment.BottomStart).height(20.dp).fillMaxWidth().clip(
                    RoundedCornerShape(50)
                ).padding(vertical = 4.dp), color = Color.Cyan)
                brandBadge(constraints)
            }
        }
        //Checking/Updating:
        if (file != "")
            Box(modifier = Modifier.offset(x = 110.dp, y = 75.dp).fillMaxWidth(.7f)) {
                if (file == "Checking for updates...")
                    Text(text = file, color = Color.Cyan, fontSize = 14.sp, fontFamily = Font.robotoFont, maxLines = 2)
                else
                    Text(text = "Updating: $file", color = Color.Cyan, fontSize = 14.sp, fontFamily = Font.robotoFont, maxLines = 2)
            }
        if (!startedThread) {
            Thread {
                Updater.update()
            }.start()
            startedThread = true
        }
    }
}