@file:OptIn(ExperimentalFoundationApi::class)

package com.animeav.tv.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AnimeRed = Color(0xFFE53935)
val AnimeRedSoft = Color(0xFFFF6B5E)
val Background = Color(0xFF09090C)
val SurfaceDark = Color(0xFF15151D)
val SurfaceElevated = Color(0xFF20202B)
val TextPrimary = Color(0xFFF4F4F7)
val TextSecondary = Color(0xFFA2A2B4)

private val AnimeColors = darkColorScheme(
    primary = AnimeRed,
    onPrimary = Color.White,
    primaryContainer = AnimeRed,
    onPrimaryContainer = Color.White,
    secondary = AnimeRedSoft,
    onSecondary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3A3A4A),
)

private val AnimeTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

/**
 * En TV el foco debe moverse al instante. El BringIntoViewSpec por defecto de Android
 * recoloca el elemento enfocado al 30% del viewport con una animación de 125 ms en
 * cada pulsación, lo que provoca un redibujado completo de la pantalla durante ~16
 * fotogramas. Aquí solo desplazamos lo mínimo para que el elemento sea visible, con
 * un margen del 12 % para que el foco no quede pegado al borde, y sin animación.
 */
private object InstantBringIntoViewSpec : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = snap()

    override fun calculateScrollDistance(offset: Float, size: Float, viewportSize: Float): Float {
        val margin = viewportSize * 0.12f
        val start = offset
        val end = offset + size
        return when {
            size >= viewportSize - margin * 2f -> start
            start < margin -> start - margin
            end > viewportSize - margin -> end - (viewportSize - margin)
            else -> 0f
        }
    }
}

@Composable
fun AnimeAvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides InstantBringIntoViewSpec) {
        MaterialTheme(
            colorScheme = AnimeColors,
            typography = AnimeTypography,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Background,
                contentColor = TextPrimary,
            ) {
                content()
            }
        }
    }
}
