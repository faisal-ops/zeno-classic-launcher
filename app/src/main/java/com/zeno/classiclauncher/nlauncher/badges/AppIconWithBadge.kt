package com.zeno.classiclauncher.nlauncher.badges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val BadgeRed = Color(0xFFE30B17)

/** Ray length and stroke width as a fraction of the badge circle's radius — matches the
 *  hand-tuned proportions from the reference design (5 rays, 72° apart, rounded caps). */
private const val RAY_LENGTH_FRACTION = 0.64f
private const val RAY_STROKE_FRACTION = 0.26f

/**
 * BlackBerry-style presence badge: small red circle with a 5-ray asterisk on the icon's
 * top-end when [hasUnread] is true. No counts, no animation.
 *
 * The asterisk is drawn as geometry (5 lines radiating from center, 72° apart, rounded caps),
 * not a Unicode glyph rendered as text — a font's "✱" character varies in ray count, spacing,
 * and weight by device/OS/font, so the previous text-based badge could look different across
 * devices. Drawing it directly keeps it pixel-identical everywhere.
 */
@Composable
fun AppIconWithBadge(
    hasUnread: Boolean,
    modifier: Modifier = Modifier,
    badgeDiameter: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (hasUnread) {
            NotificationBadgeDot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp),
                diameter = badgeDiameter,
            )
        }
    }
}

/** The red circle + 5-ray asterisk badge on its own, for callers that position it themselves
 *  (e.g. folder-group icon stacks) rather than going through [AppIconWithBadge]. */
@Composable
fun NotificationBadgeDot(
    modifier: Modifier = Modifier,
    diameter: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(BadgeRed),
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val radius = size.minDimension / 2f
            val rayLength = radius * RAY_LENGTH_FRACTION
            val strokeWidth = radius * RAY_STROKE_FRACTION
            val center = Offset(size.width / 2f, size.height / 2f)
            for (i in 0 until 5) {
                val angleDegrees = -90.0 + 72.0 * i
                val angleRadians = Math.toRadians(angleDegrees)
                val end = Offset(
                    x = center.x + (rayLength * cos(angleRadians)).toFloat(),
                    y = center.y + (rayLength * sin(angleRadians)).toFloat(),
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
