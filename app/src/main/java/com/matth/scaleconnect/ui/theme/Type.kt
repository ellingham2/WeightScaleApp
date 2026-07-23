package com.matth.scaleconnect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.R

@OptIn(ExperimentalTextApi::class)
val HankenGrotesk = FontFamily(
    Font(
        R.font.hanken_grotesk, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.hanken_grotesk, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.hanken_grotesk, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.hanken_grotesk, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
)

@OptIn(ExperimentalTextApi::class)
val BricolageGrotesque = FontFamily(
    Font(
        R.font.bricolage_grotesque, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600), FontVariation.Setting("opsz", 36f)
        )
    ),
    Font(
        R.font.bricolage_grotesque, FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700), FontVariation.Setting("opsz", 36f)
        )
    ),
)

val ScaleConnectTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        letterSpacing = (-0.03f).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.01f).em,
    ),
    titleLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.09f.em,
    ),
)
