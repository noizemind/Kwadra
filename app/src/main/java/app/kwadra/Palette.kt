package app.kwadra

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** Те же роли Material, что в веб-теме: оттенок акцента, светлая и тёмная схема. */
data class Palette(
    val dark: Boolean,
    val surface: Color,
    val surfaceDim: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val ink: Color,
    val muted: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val error: Color,
    val inverse: Color,
    val onInverse: Color,
    val pieces: List<Color>,
) {
    companion object {
        fun fromSeed(dark: Boolean, hue: Float, chroma: Float): Palette {
            val h = if (hue.isFinite()) hue else 170f
            val c = min(0.12f, max(0.05f, if (chroma > 0f) chroma else 0.09f))
            val pieceC = min(0.14f, max(0.07f, c * 1.2f))
            val hues = floatArrayOf(h, h + 28, h - 48, h + 16, h - 22, h + 44)
            return Palette(
                dark = dark,
                surface = role(dark, h, 0.985f, 0.145f, 0.012f, 0.014f),
                surfaceDim = role(dark, h, 0.94f, 0.11f, 0.012f, 0.012f),
                container = role(dark, h, 0.95f, 0.2f, 0.013f, 0.015f),
                low = role(dark, h, 0.965f, 0.175f, 0.012f, 0.014f),
                high = role(dark, h, 0.93f, 0.23f, 0.014f, 0.016f),
                highest = role(dark, h, 0.91f, 0.265f, 0.015f, 0.018f),
                ink = role(dark, h, 0.2f, 0.92f, 0.02f, 0.015f),
                muted = role(dark, h, 0.42f, 0.76f, 0.02f, 0.02f),
                outlineVariant = role(dark, h, 0.8f, 0.36f, 0.02f, 0.02f),
                primary = role(dark, h, 0.4f, 0.82f, c, c * 0.85f),
                onPrimary = onRole(dark, h, !dark),
                primaryContainer = role(dark, h, 0.88f, 0.34f, c * 0.7f, c * 0.7f),
                onPrimaryContainer = role(dark, h, 0.22f, 0.92f, c * 0.35f, c * 0.2f),
                secondaryContainer = role(dark, h + 28f, 0.9f, 0.32f, c * 0.35f, c * 0.35f),
                onSecondaryContainer = role(dark, h + 28f, 0.24f, 0.92f, c * 0.2f, c * 0.12f),
                error = role(dark, 25f, 0.5f, 0.78f, 0.16f, 0.12f),
                inverse = role(dark, h, 0.22f, 0.9f, 0.016f, 0.012f),
                onInverse = role(dark, h, 0.92f, 0.22f, 0.012f, 0.016f),
                pieces = hues.map { hue -> role(dark, hue, 0.70f, 0.72f, pieceC, pieceC * 0.92f) },
            )
        }

        private fun role(dark: Boolean, h: Float, lightL: Float, darkL: Float, lightC: Float, darkC: Float): Color {
            return oklch(if (dark) darkL else lightL, if (dark) darkC else lightC, h)
        }

        private fun onRole(dark: Boolean, h: Float, forDarkBg: Boolean): Color {
            return if (forDarkBg) oklch(if (dark) 0.92f else 0.98f, 0.02f, h)
            else oklch(if (dark) 0.28f else 0.22f, 0.04f, h)
        }
    }
}

fun oklch(l: Float, c: Float, h: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val a = c * cos(hue * PI / 180.0)
    val b = c * sin(hue * PI / 180.0)
    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b
    val ll = l_.pow(3.0)
    val mm = m_.pow(3.0)
    val ss = s_.pow(3.0)
    val r = +4.0767416621 * ll - 3.3077115913 * mm + 0.2309699292 * ss
    val g = -1.2684380046 * ll + 2.6097574011 * mm - 0.3413193965 * ss
    val bl = -0.0041960863 * ll - 0.7034186147 * mm + 1.7076147010 * ss
    return Color(srgb(r), srgb(g), srgb(bl))
}

fun rgbToOklch(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    fun lin(c: Int): Double {
        val x = c / 255.0
        return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
    }
    val lr = lin(r)
    val lg = lin(g)
    val lb = lin(b)
    val l = 0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb
    val m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb
    val s = 0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb
    val l_ = l.pow(1.0 / 3.0)
    val m_ = m.pow(1.0 / 3.0)
    val s_ = s.pow(1.0 / 3.0)
    val L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_
    val A = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_
    val B = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
    var H = atan2(B, A) * 180.0 / PI
    if (H < 0) H += 360.0
    return Triple(L.toFloat(), hypot(A, B).toFloat(), H.toFloat())
}

private fun srgb(linear: Double): Int {
    val v = if (linear <= 0.0031308) 12.92 * linear else 1.055 * linear.pow(1.0 / 2.4) - 0.055
    return (min(1.0, max(0.0, v)) * 255.0).roundToInt()
}
