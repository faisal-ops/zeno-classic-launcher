package com.zeno.classiclauncher.nlauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zeno.classiclauncher.nlauncher.R

val SlatePro = FontFamily(
    Font(R.font.slatepro_regular, weight = FontWeight.Normal),
    Font(R.font.slatepro_bold, weight = FontWeight.Bold),
)

val BbTypography = Typography(
    bodyMedium = TextStyle(fontFamily = SlatePro),
    bodySmall = TextStyle(fontFamily = SlatePro),
    labelMedium = TextStyle(fontFamily = SlatePro),
    labelSmall = TextStyle(fontFamily = SlatePro),
    titleMedium = TextStyle(fontFamily = SlatePro, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = SlatePro, fontWeight = FontWeight.Bold),
)

@Composable
fun BbTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = BbTypography,
        content = content,
    )
}

