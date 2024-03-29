package meteor// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import meteor.ui.UI.launcher

object Main {
    @JvmStatic
    fun main(args: Array<String>) = application {
        Window(onCloseRequest = ::exitApplication, state = WindowState(size = WindowSize(500.dp, 130.dp), position = WindowPosition(alignment = Alignment.Center)), undecorated = true, transparent = true, resizable = false) {
            launcher()
        }
    }
}


