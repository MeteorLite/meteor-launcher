package meteor.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

object Font {
    val robotoFont = FontFamily(
        Font(
            resource = "Roboto-Regular.ttf",
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )
    )
}