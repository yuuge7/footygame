package com.example.footygame.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.footygame.R

/** Big Shoulders Display: tall, condensed stadium-signage numerals for scores and titles. */
val DisplayFamily = FontFamily(
    Font(R.font.bigshoulders_extrabold, FontWeight.ExtraBold),
    Font(R.font.bigshoulders_black, FontWeight.Black),
)

/** Barlow: plain-spoken grotesk for names, copy and labels. */
val BodyFamily = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 96.sp, lineHeight = 84.sp, letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 60.sp, lineHeight = 56.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 44.sp, lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp, lineHeight = 26.sp, letterSpacing = 0.3.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp,
    ),
)
