package com.diegolopmon.freevolaba.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.db.WorkoutSessionEntity
import com.diegolopmon.freevolaba.training.Chronometer
import com.diegolopmon.freevolaba.ui.theme.FreeVolabaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    sessions: List<WorkoutSessionEntity>,
    onSessionClick: (WorkoutSessionEntity) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.empty_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    WorkoutSessionCard(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionCard(
    session: WorkoutSessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp))

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and Time
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(64.dp))

            // Concise metrics row: Duration, Calories, Avg Power
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricBadge(
                    label = stringResource(R.string.label_time),
                    value = Chronometer.format(session.durationSeconds)
                )
                MetricBadge(
                    label = stringResource(R.string.label_calories),
                    value = "${session.totalCalories} kcal"
                )
                MetricBadge(
                    label = stringResource(R.string.label_power),
                    value = "${session.avgPower} W"
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(
    name = "History Screen Preview",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun WorkoutHistoryScreenPreview() {
    val sampleSessions = listOf(
        WorkoutSessionEntity(
            id = 1,
            timestamp = System.currentTimeMillis() - 86400000L,
            durationSeconds = 625L,
            totalDistance = 2400,
            totalCalories = 146,
            avgPower = 165,
            maxPower = 280,
            avgRpm = 82,
            maxRpm = 115,
            avgResistance = 4.5
        ),
        WorkoutSessionEntity(
            id = 2,
            timestamp = System.currentTimeMillis() - 172800000L,
            durationSeconds = 1200L,
            totalDistance = 5100,
            totalCalories = 310,
            avgPower = 180,
            maxPower = 310,
            avgRpm = 88,
            maxRpm = 120,
            avgResistance = 5.0
        )
    )
    FreeVolabaTheme {
        WorkoutHistoryScreen(
            sessions = sampleSessions,
            onSessionClick = {},
            onBackClick = {}
        )
    }
}