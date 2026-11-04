package com.diegolopmon.freevolaba.ui

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.db.WorkoutSessionEntity
import com.diegolopmon.freevolaba.ui.theme.FreeVolabaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class XAxisPeriod {
    WEEKS, MONTHS
}

enum class YAxisMetric {
    CALORIES, TIME
}

data class BarChartPoint(
    val label: String,
    val value: Float,
    val displayValue: String
)

@Composable
fun WorkoutHistoryBarChart(
    sessions: List<WorkoutSessionEntity>,
    modifier: Modifier = Modifier
) {
    var period by remember { mutableStateOf(XAxisPeriod.WEEKS) }
    var metric by remember { mutableStateOf(YAxisMetric.CALORIES) }

    val dataPoints = remember(sessions, period, metric) {
        aggregateSessions(sessions, period, metric, Locale.getDefault())
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Controls: Title & Segmented Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chart_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Selectors Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // X-Axis Selector: Weeks / Months
                    PillSegmentedControl(
                        options = listOf(
                            XAxisPeriod.WEEKS to stringResource(R.string.label_weeks),
                            XAxisPeriod.MONTHS to stringResource(R.string.label_months)
                        ),
                        selectedOption = period,
                        onOptionSelected = { period = it }
                    )

                    // Y-Axis Selector: Calories / Time
                    PillSegmentedControl(
                        options = listOf(
                            YAxisMetric.CALORIES to stringResource(R.string.label_calories_short),
                            YAxisMetric.TIME to stringResource(R.string.label_time_short)
                        ),
                        selectedOption = metric,
                        onOptionSelected = { metric = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dataPoints.isEmpty() || dataPoints.all { it.value == 0f }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_chart_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paddingBottom = 32.dp.toPx()
                        val paddingTop = 28.dp.toPx()
                        val paddingLeft = 12.dp.toPx()
                        val paddingRight = 12.dp.toPx()

                        val chartHeight = canvasHeight - paddingTop - paddingBottom
                        val chartWidth = canvasWidth - paddingLeft - paddingRight

                        val maxValue = (dataPoints.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

                        val textPaint = Paint().apply {
                            color = textColor
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }

                        val valTextPaint = Paint().apply {
                            color = primaryColor.toArgb()
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }

                        // Grid line 0%
                        val y0 = canvasHeight - paddingBottom
                        drawLine(outlineColor, Offset(paddingLeft, y0), Offset(canvasWidth - paddingRight, y0), strokeWidth = 1.dp.toPx())

                        // Grid line 50%
                        val y50 = paddingTop + chartHeight / 2f
                        drawLine(outlineColor.copy(alpha = 0.5f), Offset(paddingLeft, y50), Offset(canvasWidth - paddingRight, y50), strokeWidth = 1.dp.toPx())

                        // Grid line 100%
                        val y100 = paddingTop
                        drawLine(outlineColor.copy(alpha = 0.3f), Offset(paddingLeft, y100), Offset(canvasWidth - paddingRight, y100), strokeWidth = 1.dp.toPx())

                        // Draw Bars
                        val numBars = dataPoints.size
                        val totalSpacing = chartWidth * 0.35f
                        val barSpacing = totalSpacing / (numBars + 1)
                        val barWidth = (chartWidth - totalSpacing) / numBars

                        dataPoints.forEachIndexed { index, point ->
                            val barLeft = paddingLeft + barSpacing + index * (barWidth + barSpacing)
                            val barHeightRatio = (point.value / maxValue).coerceIn(0f, 1f)
                            val barHeight = chartHeight * barHeightRatio
                            val barTop = y0 - barHeight

                            // Draw vertical bar with rounded top corners
                            drawRoundRect(
                                color = if (point.value > 0f) primaryColor else outlineColor.copy(alpha = 0.2f),
                                topLeft = Offset(barLeft, if (point.value > 0f) barTop else y0 - 4.dp.toPx()),
                                size = Size(barWidth, if (point.value > 0f) barHeight else 4.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Value text above bar
                            if (point.value > 0f) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    point.displayValue,
                                    barLeft + barWidth / 2f,
                                    barTop - 6.dp.toPx(),
                                    valTextPaint
                                )
                            }

                            // Label below bar
                            drawContext.canvas.nativeCanvas.drawText(
                                point.label,
                                barLeft + barWidth / 2f,
                                y0 + 20.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> PillSegmentedControl(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selectedOption
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOptionSelected(option) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

fun aggregateSessions(
    sessions: List<WorkoutSessionEntity>,
    period: XAxisPeriod,
    metric: YAxisMetric,
    locale: Locale
): List<BarChartPoint> {
    if (sessions.isEmpty()) return emptyList()

    val sortedSessions = sessions.sortedBy { it.timestamp }
    val calendar = Calendar.getInstance(locale)

    return when (period) {
        XAxisPeriod.WEEKS -> {
            val now = System.currentTimeMillis()
            val latestSessionTime = if (sessions.isNotEmpty()) sessions.maxOf { it.timestamp } else now
            val anchorTime = if (latestSessionTime > now - 7 * 86400000L) now else latestSessionTime

            calendar.timeInMillis = anchorTime
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val mondayMillis = calendar.timeInMillis
            val dayMillis = 86400000L
            val dayFormat = SimpleDateFormat("EEE", locale)

            (0..6).map { dayIndex ->
                val dayStart = mondayMillis + dayIndex * dayMillis
                val dayEnd = dayStart + dayMillis - 1

                val daySessions = sessions.filter { it.timestamp in dayStart..dayEnd }

                calendar.timeInMillis = dayStart
                val label = dayFormat.format(Date(dayStart))
                    .replace(".", "")
                    .replaceFirstChar { it.uppercase() }

                val totalValue = when (metric) {
                    YAxisMetric.CALORIES -> daySessions.sumOf { it.totalCalories }.toFloat()
                    YAxisMetric.TIME -> daySessions.sumOf { it.durationSeconds } / 60f
                }

                val displayValue = when (metric) {
                    YAxisMetric.CALORIES -> if (totalValue > 0f) "${totalValue.toInt()} kcal" else ""
                    YAxisMetric.TIME -> {
                        val mins = totalValue.toInt()
                        if (mins > 0) {
                            if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                        } else ""
                    }
                }

                BarChartPoint(label = label, value = totalValue, displayValue = displayValue)
            }
        }
        XAxisPeriod.MONTHS -> {
            val monthFormat = SimpleDateFormat("MMM", locale)
            val groups = sortedSessions.groupBy { session ->
                calendar.timeInMillis = session.timestamp
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                "$year-$month"
            }

            groups.entries.toList().takeLast(6).map { entry ->
                val sessionList = entry.value
                val firstSessionTime = sessionList.first().timestamp
                val label = monthFormat.format(Date(firstSessionTime)).replaceFirstChar { it.uppercase() }

                val totalValue = when (metric) {
                    YAxisMetric.CALORIES -> sessionList.sumOf { it.totalCalories }.toFloat()
                    YAxisMetric.TIME -> sessionList.sumOf { it.durationSeconds } / 60f
                }

                val displayValue = when (metric) {
                    YAxisMetric.CALORIES -> "${totalValue.toInt()} kcal"
                    YAxisMetric.TIME -> {
                        val mins = totalValue.toInt()
                        if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                    }
                }

                BarChartPoint(label = label, value = totalValue, displayValue = displayValue)
            }
        }
    }
}

@Preview(
    name = "Gráfico de Barras Lunes a Domingo",
    device = "spec:width=600dp,height=400dp,dpi=240",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun WorkoutHistoryBarChartPreview() {
    val dayMillis = 86400000L
    val now = System.currentTimeMillis()
    val sampleSessions = listOf(
        WorkoutSessionEntity(id = 1, userId = 1, timestamp = now - 6 * dayMillis, durationSeconds = 1800L, totalDistance = 8500, totalCalories = 420, avgPower = 175, maxPower = 290, avgRpm = 84, maxRpm = 118, avgResistance = 4.8),
        WorkoutSessionEntity(id = 2, userId = 1, timestamp = now - 4 * dayMillis, durationSeconds = 2400L, totalDistance = 11200, totalCalories = 580, avgPower = 190, maxPower = 310, avgRpm = 88, maxRpm = 122, avgResistance = 5.2),
        WorkoutSessionEntity(id = 3, userId = 1, timestamp = now - 2 * dayMillis, durationSeconds = 1500L, totalDistance = 7100, totalCalories = 360, avgPower = 168, maxPower = 270, avgRpm = 80, maxRpm = 112, avgResistance = 4.5),
        WorkoutSessionEntity(id = 4, userId = 1, timestamp = now - 1 * dayMillis, durationSeconds = 2700L, totalDistance = 13500, totalCalories = 690, avgPower = 205, maxPower = 340, avgRpm = 92, maxRpm = 128, avgResistance = 5.8)
    )
    FreeVolabaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            WorkoutHistoryBarChart(
                sessions = sampleSessions,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
