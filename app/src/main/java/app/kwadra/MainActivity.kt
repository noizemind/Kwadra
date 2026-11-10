package app.kwadra

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(this)
        val game = GameController(store, Tones(), Buzz(this))
        val tags = (0 until resources.configuration.locales.size()).map {
            resources.configuration.locales[it].toLanguageTag()
        }
        game.languageTags = tags
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) game.persist()
            },
        )
        setContent { KwadraScreen(game) }
    }
}

@Composable
fun paletteFor(theme: String): Palette {
    val context = LocalContext.current
    val night = when (theme) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val seed = remember(night) { accentSeed(context, night) }
    return Palette.fromSeed(night, seed.third, seed.second)
}

private fun accentSeed(context: android.content.Context, dark: Boolean): Triple<Float, Float, Float> {
    if (Build.VERSION.SDK_INT >= 31) {
        try {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            return rgbToOklch(scheme.primary)
        } catch (_: Exception) {
        }
    }
    return Triple(0.48f, 0.09f, 170f)
}

private fun rgbToOklch(color: Color): Triple<Float, Float, Float> {
    return rgbToOklch(
        (color.red * 255f).roundToInt(),
        (color.green * 255f).roundToInt(),
        (color.blue * 255f).roundToInt(),
    )
}
