package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.R

// Plus Jakarta Sans for interface text. Fraunces for the app wordmark.
// Both are SIL Open Font License.
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val PlusJakartaSans = FontFamily(
    variableFont(R.font.plus_jakarta_sans, FontWeight.Normal),
    variableFont(R.font.plus_jakarta_sans, FontWeight.Medium),
    variableFont(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    variableFont(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val Fraunces = FontFamily(
    variableFont(R.font.fraunces, FontWeight.SemiBold),
    variableFont(R.font.fraunces, FontWeight.Bold),
)

private fun TextStyle.withAppType(): TextStyle = copy(fontFamily = PlusJakartaSans)

private fun Typography.withAppType() = copy(
    displayLarge = displayLarge.withAppType(),
    displayMedium = displayMedium.withAppType(),
    displaySmall = displaySmall.withAppType(),
    headlineLarge = headlineLarge.withAppType(),
    headlineMedium = headlineMedium.withAppType(),
    headlineSmall = headlineSmall.withAppType(),
    titleLarge = titleLarge.withAppType(),
    titleMedium = titleMedium.withAppType(),
    titleSmall = titleSmall.withAppType(),
    bodyLarge = bodyLarge.withAppType(),
    bodyMedium = bodyMedium.withAppType(),
    bodySmall = bodySmall.withAppType(),
    labelLarge = labelLarge.withAppType(),
    labelMedium = labelMedium.withAppType(),
    labelSmall = labelSmall.withAppType(),
)

val Typography = Typography().withAppType()
