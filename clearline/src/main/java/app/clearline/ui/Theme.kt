package app.clearline.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Frutiger Aero–inspired Clearline palette: sky blues, glass, glossy highlights.
 * Original softphone look — not a copy of any commercial messenger.
 */
data class ClearColors(
    val ink: Color = Color(0xFF0A3A5C),
    val inkSoft: Color = Color(0xFF1B5F8A),
    val mist: Color = Color(0xFFE8F6FF),
    val mistDeep: Color = Color(0xFFC9E9FF),
    val foam: Color = Color(0xFFF5FBFF),
    val sky: Color = Color(0xFF2EA8E5),
    val skyBright: Color = Color(0xFF5CC8FF),
    val aqua: Color = Color(0xFF7FE3F0),
    val glass: Color = Color(0xCCFFFFFF),
    val glassEdge: Color = Color(0x99FFFFFF),
    val coral: Color = Color(0xFFE85D6A),
    val muted: Color = Color(0xFF5A7A94),
    val line: Color = Color(0xFFB5D8F0),
    val ok: Color = Color(0xFF3DCF8E),
    val warn: Color = Color(0xFFF0B429),
    // keep teal aliases pointing at sky so older call sites stay coherent
    val teal: Color = Color(0xFF2EA8E5),
    val tealBright: Color = Color(0xFF5CC8FF),
) {
    val heroBrush: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFF0B6BB5), Color(0xFF1E9BE0), Color(0xFF6ED6F5)),
        )

    val pageBrush: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color(0xFFF7FCFF),
                Color(0xFFD9F0FF),
                Color(0xFFB8E4FF),
                Color(0xFF9ED4F7),
            ),
        )

    val glossBrush: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xEEFFFFFF), Color(0x33FFFFFF), Color(0x11A0D8FF)),
        )

    val orbBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(Color(0xAAFFFFFF), Color(0x55A8E8FF), Color(0x00FFFFFF)),
            center = Offset.Unspecified,
            radius = 400f,
        )
}

data class ClearType(
    val brand: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    val dial: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        letterSpacing = 1.sp,
    ),
)

val LocalClearColors = staticCompositionLocalOf { ClearColors() }
val LocalClearType = staticCompositionLocalOf { ClearType() }

@Composable
fun ClearlineTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalClearColors provides ClearColors(),
        LocalClearType provides ClearType(),
        content = content,
    )
}
