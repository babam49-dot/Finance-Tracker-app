package com.example.ethfinance.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class EthExtendedColors(
    val cardBorder: Color,
    val textLabel: Color,
    val incomeGreen: Color,
    val expenseRed: Color,
    val reviewOrange: Color,
    val reviewBannerBg: Color,
    val reviewBannerBorder: Color,
    val cashGold: Color,
    val cbeAccent: Color,
    val cbeAccentBg: Color,
    val telebirrAccent: Color,
    val telebirrAccentBg: Color,
    val sampleTag: Color,
    val divider: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    EthExtendedColors(
        cardBorder = CardBorderLight,
        textLabel = TextLabelLight,
        incomeGreen = IncomeGreenLight,
        expenseRed = ExpenseRedLight,
        reviewOrange = ReviewOrangeLight,
        reviewBannerBg = ReviewBannerBgLight,
        reviewBannerBorder = ReviewBannerBorderLight,
        cashGold = CashGoldLight,
        cbeAccent = CbeMaroon,
        cbeAccentBg = CbeMaroonBg,
        telebirrAccent = TelebirrBlue,
        telebirrAccentBg = TelebirrBlueBg,
        sampleTag = SampleTagColor,
        divider = DividerLight
    )
}

val EthShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryGreenDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDarkTheme,
    onPrimary = Color.Black,
    primaryContainer = PrimaryContainerDarkTheme,
    onPrimaryContainer = PrimaryGreenDarkTheme,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark
)

@Composable
fun EthFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) {
        EthExtendedColors(
            cardBorder = CardBorderDark,
            textLabel = TextLabelDark,
            incomeGreen = IncomeGreenDark,
            expenseRed = ExpenseRedDark,
            reviewOrange = ReviewOrangeDark,
            reviewBannerBg = ReviewBannerBgDark,
            reviewBannerBorder = ReviewBannerBorderDark,
            cashGold = CashGoldDark,
            cbeAccent = Color(0xFFC070AC),
            cbeAccentBg = Color(0xFF381530),
            telebirrAccent = Color(0xFF64B5F6),
            telebirrAccentBg = Color(0xFF132B45),
            sampleTag = CashGoldDark,
            divider = DividerDark
        )
    } else {
        EthExtendedColors(
            cardBorder = CardBorderLight,
            textLabel = TextLabelLight,
            incomeGreen = IncomeGreenLight,
            expenseRed = ExpenseRedLight,
            reviewOrange = ReviewOrangeLight,
            reviewBannerBg = ReviewBannerBgLight,
            reviewBannerBorder = ReviewBannerBorderLight,
            cashGold = CashGoldLight,
            cbeAccent = CbeMaroon,
            cbeAccentBg = CbeMaroonBg,
            telebirrAccent = TelebirrBlue,
            telebirrAccentBg = TelebirrBlueBg,
            sampleTag = SampleTagColor,
            divider = DividerLight
        )
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EthTypography,
            shapes = EthShapes,
            content = content
        )
    }
}
