package app.clearline.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class ClearColors(
    val ink: Color = Color(0xFF0B1F2A),
    val inkSoft: Color = Color(0xFF1A3442),
    val mist: Color = Color(0xFFE7F2F4),
    val mistDeep: Color = Color(0xFFD2E4E8),
    val foam: Color = Color(0xFFF7FBFC),
    val teal: Color = Color(0xFF0F766E),
    val tealBright: Color = Color(0xFF14B8A6),
    val coral: Color = Color(0xFFC45C4A),
    val muted: Color = Color(0xFF5B7380),
    val line: Color = Color(0xFFB7CBD2),
    val ok: Color = Color(0xFF1F8A70),
    val warn: Color = Color(0xFFB7791F),
) {
    val heroBrush: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFF0B1F2A), Color(0xFF123645), Color(0xFF0F766E)),
        )
    val pageBrush: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFF7FBFC), Color(0xFFE7F2F4), Color(0xFFDCEBEF)),
        )
}

data class ClearType(
    val brand: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp,
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
        fontWeight = FontWeight.Medium,
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
