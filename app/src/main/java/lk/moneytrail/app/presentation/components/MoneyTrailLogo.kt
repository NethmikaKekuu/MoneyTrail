package lk.moneytrail.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import lk.moneytrail.app.presentation.theme.TrailOrange

/**
 * The MoneyTrail mark: a dotted trail leading to a pin. Drawn on a 64-unit grid,
 * copied from the SVG in MoneyTrail-Figma-HTML/25-Logo.html. Vector drawables can't
 * draw the dotted line, so it's a Canvas.
 *
 * Use [reversed] on orange backgrounds: white tile, orange trail.
 */
@Composable
fun MoneyTrailLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    reversed: Boolean = false
) {
    val tile = if (reversed) Color.White else TrailOrange
    val mark = if (reversed) TrailOrange else Color.White

    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.width / 64f
        drawRoundRect(color = tile, cornerRadius = CornerRadius(18 * unit))

        // SVG: M13 49 C 25 49, 22 33, 32 33 S 38 24, 41 22
        val trail = Path().apply {
            moveTo(13 * unit, 49 * unit)
            cubicTo(25 * unit, 49 * unit, 22 * unit, 33 * unit, 32 * unit, 33 * unit)
            cubicTo(42 * unit, 33 * unit, 38 * unit, 24 * unit, 41 * unit, 22 * unit)
        }
        drawPath(
            path = trail,
            color = mark,
            style = Stroke(
                width = 4.5f * unit,
                cap = StrokeCap.Round,
                // Near-zero dashes with round caps draw as dots.
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f * unit, 7.5f * unit))
            )
        )

        val pin = Offset(48 * unit, 17 * unit)
        drawCircle(color = mark, radius = 8 * unit, center = pin)
        drawCircle(color = tile, radius = 3.6f * unit, center = pin, style = Stroke(width = 2 * unit))
    }
}

@Preview(showBackground = true)
@Composable
private fun MoneyTrailLogoPreview() {
    MoneyTrailTheme {
        Row {
            MoneyTrailLogo(size = 96.dp)
            MoneyTrailLogo(size = 96.dp, reversed = true)
        }
    }
}
