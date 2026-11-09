package com.listenstats.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/*
 * ============================================================================
 *  EVERYTHING VISUAL LIVES IN THIS FILE: colors, shapes, typography, spacing.
 *  Restyle the whole app by editing the sections below.
 * ============================================================================
 */

// ---------------------------------------------------------------- 1. COLORS

// Light scheme
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E0FF),
    onPrimaryContainer = Color(0xFF0A0A6B),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF8E4585),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F5),
    onTertiaryContainer = Color(0xFF38003A),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9)
)

// Dark scheme
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC0C1FF),
    onPrimary = Color(0xFF21229E),
    primaryContainer = Color(0xFF3A3AC8),
    onPrimaryContainer = Color(0xFFE1E0FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFF0B4E6),
    onTertiary = Color(0xFF572054),
    tertiaryContainer = Color(0xFF72386B),
    onTertiaryContainer = Color(0xFFFFD7F5),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A)
)

// AMOLED black: applied on top of the dark scheme (also on top of dynamic color)
private fun amoledize(cs: ColorScheme): ColorScheme = cs.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF111114),
    surfaceContainerHigh = Color(0xFF18181C),
    surfaceContainerHighest = Color(0xFF202025)
)

/** Gradients for the Rewind story cards. Each pair is (top, bottom). */
object StoryColors {
    val gradients: List<Pair<Color, Color>> = listOf(
        Pair(Color(0xFF4F46E5), Color(0xFF9333EA)),
        Pair(Color(0xFFDB2777), Color(0xFFF97316)),
        Pair(Color(0xFF0891B2), Color(0xFF4F46E5)),
        Pair(Color(0xFF16A34A), Color(0xFF0891B2)),
        Pair(Color(0xFF9333EA), Color(0xFFDB2777)),
        Pair(Color(0xFFEA580C), Color(0xFFCA8A04)),
        Pair(Color(0xFF1D4ED8), Color(0xFF7C3AED))
    )
    val onStory = Color(0xFFFFFFFF)
}

// ---------------------------------------------------------------- 2. SHAPES

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(44.dp)
)

// ---------------------------------------------------------------- 3. TYPOGRAPHY

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
        fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    )
)

// ---------------------------------------------------------------- 4. SPACING / SIZES

object Dim {
    val screenPadding = 20.dp
    val gap = 12.dp
    val cardPadding = 20.dp
    val chartHeight = 140.dp
    val barGap = 4.dp
    val storyHeight = 520.dp
}

// ---------------------------------------------------------------- 5. THEME ENTRY POINT

@Composable
fun ListenStatsTheme(
    amoled: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    // AMOLED black forces the dark scheme.
    val dark = amoled || isSystemInDarkTheme()
    val context = LocalContext.current

    val base: ColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    val scheme = if (amoled) amoledize(base) else base

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
