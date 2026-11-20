package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCheckFirstColors

data class DonutSlice(
    val label: String,
    val value: Int,
    val color: Color,
)

@Composable
fun RiskDistributionDonutChart(
    slices: List<DonutSlice>,
    totalChecks: Int,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val safeTotal = totalChecks.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(188.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 22.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Dark ambient ring track
                drawArc(
                    color = c.track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )

                var startAngle = -90f
                val gapDegrees = 5f
                val activeSlices = slices.filter { it.value > 0 }
                activeSlices.forEach { slice ->
                    val rawSweep = (slice.value.toFloat() / safeTotal.toFloat()) * 360f
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(3f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    startAngle += rawSweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalChecks.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = c.navy,
                )
                Text(
                    text = com.example.ui.i18n.LocalStrings.current.checksUnit,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slices.forEach { slice ->
                val pct = ((slice.value.toDouble() / safeTotal.toDouble()) * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color),
                    )
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${slice.value} ($pct%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.navy,
                    )
                }
            }
        }
    }
}

/**
 * Smooth luminous violet curve & gradient area chart matching the reference dashboard dynamics chart.
 */
@Composable
fun SevenDayBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val maxVal = (data.maxOfOrNull { it.second } ?: 25).coerceAtLeast(25)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val gridSteps = 4
            val gridColor = c.track
            val topPad = 14.dp.toPx()
            val bottomY = size.height - 6.dp.toPx()
            val usableHeight = bottomY - topPad

            for (i in 0..gridSteps) {
                val y = bottomY - (usableHeight * i / gridSteps)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (data.size >= 2) {
                val horizontalPad = 12.dp.toPx()
                val stepX = (size.width - horizontalPad * 2) / (data.size - 1)
                val points = data.mapIndexed { idx, (_, value) ->
                    val fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val x = horizontalPad + idx * stepX
                    val y = bottomY - fraction * usableHeight
                    Offset(x, y)
                }

                val curvePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.lastIndex) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val ctrlX = (p0.x + p1.x) / 2f
                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(points.last().x, bottomY)
                    lineTo(points.first().x, bottomY)
                    close()
                }

                // Translucent cosmic violet gradient fill under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            c.accent.copy(alpha = 0.42f),
                            c.accentTint.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        startY = topPad,
                        endY = bottomY,
                    ),
                )

                // Soft outer neon violet bloom stroke
                drawPath(
                    path = curvePath,
                    color = c.accent.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 7.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Crisp luminous violet primary curve
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(c.accent, c.accentGlow, c.accentLight),
                    ),
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Subtle luminous data points
                points.forEach { pt ->
                    drawCircle(
                        color = c.accent.copy(alpha = 0.30f),
                        radius = 6.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = c.horizonCore,
                        radius = 2.8.dp.toPx(),
                        center = pt,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { (day, _) ->
                Text(text = day, style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

@Composable
fun HorizontalMetricBar(
    label: String,
    valueText: String,
    fraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = c.muted)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = c.navy)
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(c.track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor),
            )
        }
    }
}
