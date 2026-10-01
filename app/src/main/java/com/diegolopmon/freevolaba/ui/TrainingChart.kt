package com.diegolopmon.freevolaba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.training.Chronometer
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint
import com.diegolopmon.freevolaba.ui.theme.ResistanceHighColor

@Composable
fun TrainingChart(
    historyPoints: List<TrainingHistoryPoint>,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    if (historyPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.chart_waiting_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val rpmColor = MaterialTheme.colorScheme.primary
    val hrColor = MaterialTheme.colorScheme.error
    val resistanceColor = ResistanceHighColor

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chart_legend_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(color = rpmColor, label = stringResource(R.string.label_rpm))
                    LegendItem(color = hrColor, label = stringResource(R.string.label_heart_rate))
                    LegendItem(color = resistanceColor, label = stringResource(R.string.label_resistance))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val maxX = maxOf(60L, elapsedTime)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height

                // Draw grid lines
                val gridColor = Color.Gray.copy(alpha = 0.2f)
                for (i in 0..3) {
                    val y = height * i / 3
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1.dp.toPx())
                }

                if (historyPoints.size < 2) return@Canvas

                val maxRpm = historyPoints.maxOfOrNull { it.trainingData.rpm.toFloat() }?.coerceAtLeast(100f) ?: 100f
                val maxHr = historyPoints.maxOfOrNull { it.trainingData.heartRate.toFloat() }?.coerceAtLeast(200f) ?: 200f
                val minResistance = 1f
                val maxResistance = 10f

                val rpmPoints = mutableListOf<Offset>()
                val hrPoints = mutableListOf<Offset>()
                val resistancePoints = mutableListOf<Offset>()

                historyPoints.forEach { point ->
                    val x = (point.elapsedTimeSeconds.toFloat() / maxX.toFloat()).coerceIn(0f, 1f) * width
                    val data = point.trainingData

                    val yRpm = (height - (data.rpm / maxRpm) * height).coerceIn(0f, height)
                    val yHr = (height - (data.heartRate / maxHr) * height).coerceIn(0f, height)
                    
                    val normRes = ((data.resistance.toFloat() - minResistance) / (maxResistance - minResistance)).coerceIn(0f, 1f)
                    val yResistance = (height - normRes * height).coerceIn(0f, height)

                    rpmPoints.add(Offset(x, yRpm))
                    hrPoints.add(Offset(x, yHr))
                    resistancePoints.add(Offset(x, yResistance))
                }

                val rpmPath = Path().apply { addSmoothLine(rpmPoints, height) }
                val hrPath = Path().apply { addSmoothLine(hrPoints, height) }
                val resistancePath = Path().apply { addSmoothLine(resistancePoints, height) }

                // Draw paths with smooth caps and joins
                drawPath(
                    path = resistancePath,
                    color = resistanceColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = hrPath,
                    color = hrColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = rpmPath,
                    color = rpmColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw current moment indicator dots at the tip of each path
                if (rpmPoints.isNotEmpty()) {
                    val last = rpmPoints.last()
                    drawCircle(color = rpmColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = last)
                    drawCircle(color = rpmColor, radius = 4.dp.toPx(), center = last)
                    drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = last)
                }
                if (hrPoints.isNotEmpty()) {
                    val last = hrPoints.last()
                    drawCircle(color = hrColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = last)
                    drawCircle(color = hrColor, radius = 4.dp.toPx(), center = last)
                    drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = last)
                }
                if (resistancePoints.isNotEmpty()) {
                    val last = resistancePoints.last()
                    drawCircle(color = resistanceColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = last)
                    drawCircle(color = resistanceColor, radius = 4.dp.toPx(), center = last)
                    drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = last)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Time Labels using Chronometer.format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Chronometer.format(0L),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = Chronometer.format(maxX / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = Chronometer.format(maxX),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun Path.addSmoothLine(points: List<Offset>, height: Float) {
    if (points.isEmpty()) return
    moveTo(points[0].x, points[0].y.coerceIn(0f, height))
    for (i in 0 until points.size - 1) {
        val p0 = points[maxOf(0, i - 1)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[minOf(points.size - 1, i + 2)]

        // Tangent estimation using central differences
        val m1 = (p2.y - p0.y) / 2f
        val m2 = (p3.y - p1.y) / 2f

        val cp1x = p1.x + (p2.x - p1.x) / 3f
        val cp1y = (p1.y + m1 / 3f).coerceIn(0f, height)

        val cp2x = p2.x - (p2.x - p1.x) / 3f
        val cp2y = (p2.y - m2 / 3f).coerceIn(0f, height)

        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y.coerceIn(0f, height))
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
