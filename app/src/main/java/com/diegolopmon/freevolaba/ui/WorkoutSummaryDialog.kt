package com.diegolopmon.freevolaba.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.training.Chronometer
import com.diegolopmon.freevolaba.training.TrainingData
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint
import com.diegolopmon.freevolaba.training.WorkoutSummary
import com.diegolopmon.freevolaba.ui.theme.FreeVolabaTheme
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutSummaryDialog(
    summary: WorkoutSummary,
    historyPoints: List<TrainingHistoryPoint>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .height(440.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.summary_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content: Left Chart + Right Stacked Values
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Chart
                    TrainingChart(
                        historyPoints = historyPoints,
                        elapsedTime = summary.durationSeconds,
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    )

                    // Right Stacked Values
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricRow(
                                label = stringResource(R.string.label_time),
                                value = Chronometer.format(summary.durationSeconds),
                                unit = ""
                            )
                            SummaryMetricRow(
                                label = stringResource(R.string.label_distance),
                                value = summary.totalDistance.toString(),
                                unit = "m"
                            )
                            SummaryMetricRow(
                                label = stringResource(R.string.label_calories),
                                value = summary.totalCalories.toString(),
                                unit = "kcal"
                            )
                            SummaryMetricRow(
                                label = stringResource(R.string.label_power),
                                value = "${summary.avgPower} / ${summary.maxPower}",
                                unit = "W (avg/max)"
                            )
                            SummaryMetricRow(
                                label = stringResource(R.string.label_rpm),
                                value = "${summary.avgRpm} / ${summary.maxRpm}",
                                unit = "rpm (avg/max)"
                            )
                            SummaryMetricRow(
                                label = stringResource(R.string.label_avg_resistance),
                                value = String.format(Locale.getDefault(), "%.1f", summary.avgResistance),
                                unit = ""
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Summary Dialog Preview",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun WorkoutSummaryDialogPreview() {
    val mockHistory = listOf(
        TrainingHistoryPoint(Date(), 0L, TrainingData(rpm = 70, resistance = 4, heartRate = 120, power = 140)),
        TrainingHistoryPoint(Date(), 300L, TrainingData(rpm = 85, resistance = 6, heartRate = 135, power = 210)),
        TrainingHistoryPoint(Date(), 625L, TrainingData(rpm = 95, resistance = 8, heartRate = 150, power = 280))
    )
    FreeVolabaTheme {
        WorkoutSummaryDialog(
            summary = WorkoutSummary(
                durationSeconds = 625L,
                totalDistance = 2400,
                totalCalories = 146,
                avgPower = 165,
                maxPower = 280,
                avgRpm = 82,
                maxRpm = 115,
                avgResistance = 4.5
            ),
            historyPoints = mockHistory,
            onDismiss = {}
        )
    }
}
