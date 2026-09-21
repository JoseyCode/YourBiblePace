package com.example.biblepaceproject.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Book = FontFamily.Serif
private val base = Typography()

private fun TextStyle.serif() = copy(fontFamily = Book)

// Everything is set in serif, like a printed book. bodyLarge is the scripture text, so it gets generous leading.
val Typography = Typography(
    displayLarge = base.displayLarge.serif(),
    displayMedium = base.displayMedium.serif(),
    displaySmall = base.displaySmall.serif(),
    headlineLarge = base.headlineLarge.serif(),
    headlineMedium = base.headlineMedium.serif().copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    headlineSmall = base.headlineSmall.serif(),
    titleLarge = base.titleLarge.serif().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.serif(),
    titleSmall = base.titleSmall.serif(),
    bodyLarge = TextStyle(fontFamily = Book, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 31.sp, letterSpacing = 0.2.sp),
    bodyMedium = base.bodyMedium.serif(),
    bodySmall = base.bodySmall.serif(),
    labelLarge = base.labelLarge.serif(),
    labelMedium = base.labelMedium.serif(),
    labelSmall = base.labelSmall.serif(),
)
