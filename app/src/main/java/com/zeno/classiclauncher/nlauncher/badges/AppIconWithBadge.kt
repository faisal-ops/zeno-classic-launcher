package com.zeno.classiclauncher.nlauncher.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BadgeRed = Color(0xFFD32F2F)

/**
 * BlackBerry-style presence badge: small red “✱” on the icon’s top-end when [hasUnread] is true.
 * No counts, no animation.
 */
@Composable
fun AppIconWithBadge(
    hasUnread: Boolean,
    modifier: Modifier = Modifier,
    badgeDiameter: Dp = 16.dp,
    glyphSp: Float = 9f,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(badgeDiameter)
                    .clip(CircleShape)
                    .background(BadgeRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2731",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = glyphSp.sp,
                        lineHeight = glyphSp.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
