package ru.it.lorewriter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.it.lorewriter.R

val RalewayFontFamily = FontFamily(
    Font(
        resId = R.font.raleway_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.raleway_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.raleway_semi_bold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.raleway_bold,
        weight = FontWeight.Bold
    )
)

val AppTypography = Typography(

    displayLarge = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp
    ),

    displayMedium = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp
    ),

    headlineMedium = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp
    ),

    headlineSmall = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),

    titleLarge = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),

    titleMedium = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),

    titleSmall = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    bodySmall = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    labelLarge = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    labelMedium = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),

    labelSmall = TextStyle(
        fontFamily = RalewayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)