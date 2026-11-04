package com.diegolopmon.freevolaba

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.diegolopmon.freevolaba.data.UserRepository
import com.diegolopmon.freevolaba.data.WorkoutRepository
import com.diegolopmon.freevolaba.db.UserEntity
import com.diegolopmon.freevolaba.db.WorkoutSessionEntity
import com.diegolopmon.freevolaba.training.Chronometer
import com.diegolopmon.freevolaba.training.TrainingData
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint
import com.diegolopmon.freevolaba.training.TrainingService
import com.diegolopmon.freevolaba.training.WorkoutSummary
import com.diegolopmon.freevolaba.ui.TrainingChart
import com.diegolopmon.freevolaba.ui.UserDialog
import com.diegolopmon.freevolaba.ui.UserProfileButton
import com.diegolopmon.freevolaba.ui.UserSelectionScreen
import com.diegolopmon.freevolaba.ui.WorkoutHistoryScreen
import com.diegolopmon.freevolaba.ui.WorkoutSummaryDialog
import com.diegolopmon.freevolaba.ui.theme.FreeVolabaTheme
import com.diegolopmon.freevolaba.ui.theme.ResistanceHighColor
import com.diegolopmon.freevolaba.ui.theme.ResistanceLowColor
import com.diegolopmon.freevolaba.ui.theme.ResistanceMediumColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val TARGET_TRAINING_SECONDS = 600L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeVolabaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrackingControlScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TrackingControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val userRepository = remember(context) { UserRepository(context) }
    val workoutRepository = remember(context) { WorkoutRepository(context) }

    val users by userRepository.allUsers.collectAsState(initial = emptyList())
    val activeUser by userRepository.activeUser.collectAsState(initial = null)

    val currentUserId = activeUser?.id
    val savedSessions by produceState(initialValue = emptyList(), currentUserId) {
        if (currentUserId != null) {
            workoutRepository.getSessionsForUser(currentUserId).collectLatest { sessions ->
                value = sessions
            }
        } else {
            value = emptyList()
        }
    }

    val isRunning by TrainingService.isRunningState.collectAsState()
    val isPaused by TrainingService.chronometer.isPaused.collectAsState()
    val historyPoints by TrainingService.historyState.collectAsState()
    val lastSummary by TrainingService.lastSummaryState.collectAsState()

    var showUserSelectionScreen by rememberSaveable { mutableStateOf(true) }
    var showHistoryScreen by rememberSaveable { mutableStateOf(false) }
    var userToEditDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showUserEditDialog by remember { mutableStateOf(false) }
    var selectedHistoricalSummary by remember { mutableStateOf<WorkoutSummary?>(null) }

    val initialTracking = remember { TrainingService.trainingDataState.value }
    val trainingData by produceState(initialValue = initialTracking) {
        while (true) {
            value = TrainingService.trainingDataState.value
            delay(1000L.milliseconds)
        }
    }
    val elapsedTime by TrainingService.chronometer.elapsedTimeSeconds.collectAsState()
    val scope = rememberCoroutineScope()
    val closeHistory = { showHistoryScreen = false }

    val currentUser = activeUser

    if (currentUser == null || showUserSelectionScreen) {
        if (currentUser != null) {
            BackHandler {
                showUserSelectionScreen = false
            }
        }
        UserSelectionScreen(
            users = users,
            onSelectUser = { selectedUser ->
                userRepository.selectUser(selectedUser.id)
                showUserSelectionScreen = false
            },
            onCreateUser = { name, avatarId, avatarPath, originalAvatarPath, birthDate, heightCm, weightKg ->
                scope.launch {
                    userRepository.createUser(name, avatarId, avatarPath, originalAvatarPath, birthDate, heightCm, weightKg)
                    showUserSelectionScreen = false
                }
            },
            modifier = modifier
        )
    } else if (showHistoryScreen) {
        BackHandler {
            showHistoryScreen = false
        }
        WorkoutHistoryScreen(
            sessions = savedSessions,
            onSessionClick = { entity ->
                selectedHistoricalSummary = WorkoutSummary(
                    timestamp = entity.timestamp,
                    durationSeconds = entity.durationSeconds,
                    totalDistance = entity.totalDistance,
                    totalCalories = entity.totalCalories,
                    avgPower = entity.avgPower,
                    maxPower = entity.maxPower,
                    avgRpm = entity.avgRpm,
                    maxRpm = entity.maxRpm,
                    avgResistance = entity.avgResistance,
                    historyPoints = entity.historyPoints
                )
            },
            onDeleteSession = { id ->
                scope.launch { workoutRepository.deleteSessionById(id) }
            },
            onBackClick = closeHistory,
            modifier = modifier
        )
    } else {
        TrackingControlContent(
            activeUser = currentUser,
            isRunning = isRunning,
            isPaused = isPaused,
            trainingData = trainingData,
            elapsedTime = elapsedTime,
            historyPoints = historyPoints,
            onStart = {
                val intent = Intent(context, TrainingService::class.java)
                ContextCompat.startForegroundService(context, intent)
            },
            onPauseToggle = {
                if (isPaused) {
                    TrainingService.resume(context)
                } else {
                    TrainingService.pause(context)
                }
            },
            onStop = {
                TrainingService.stopAndSummarize(context, currentUser.id)
            },
            onSwitchUser = {
                showUserSelectionScreen = true
            },
            onEditProfile = {
                userToEditDialog = currentUser
                showUserEditDialog = true
            },
            onCreateNewUser = {
                userToEditDialog = null
                showUserEditDialog = true
            },
            onOpenHistory = {
                showHistoryScreen = true
            },
            modifier = modifier
        )
    }

    if (showUserEditDialog) {
        UserDialog(
            userToEdit = userToEditDialog,
            onDismiss = {
                showUserEditDialog = false
                userToEditDialog = null
            },
            onSave = { newName, newAvatarId, avatarPath, originalAvatarPath, birthDate, heightCm, weightKg ->
                val current = userToEditDialog
                if (current != null) {
                    scope.launch {
                        userRepository.updateUser(
                            current.copy(
                                name = newName,
                                avatarId = newAvatarId,
                                avatarPath = avatarPath,
                                originalAvatarPath = originalAvatarPath,
                                birthDate = birthDate,
                                heightCm = heightCm,
                                weightKg = weightKg
                            )
                        )
                    }
                } else {
                    scope.launch {
                        userRepository.createUser(
                            name = newName,
                            avatarId = newAvatarId,
                            avatarPath = avatarPath,
                            originalAvatarPath = originalAvatarPath,
                            birthDate = birthDate,
                            heightCm = heightCm,
                            weightKg = weightKg
                        )
                    }
                }
                showUserEditDialog = false
                userToEditDialog = null
            },
            onDelete = { userToDelete ->
                scope.launch {
                    userRepository.deleteUser(userToDelete.id)
                }
                showUserEditDialog = false
                userToEditDialog = null
            }
        )
    }

    val summaryToDisplay = lastSummary ?: selectedHistoricalSummary
    summaryToDisplay?.let { summary ->
        WorkoutSummaryDialog(
            summary = summary,
            historyPoints = summary.historyPoints,
            onDismiss = {
                if (lastSummary != null) {
                    TrainingService.clearSummary()
                }
                selectedHistoricalSummary = null
            }
        )
    }
}

@Composable
fun TrackingControlContent(
    activeUser: UserEntity,
    isRunning: Boolean,
    isPaused: Boolean,
    trainingData: TrainingData,
    elapsedTime: Long,
    historyPoints: List<TrainingHistoryPoint>,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onSwitchUser: () -> Unit,
    onEditProfile: () -> Unit,
    onCreateNewUser: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Top-right header controls: User profile button AND separate 3-dots overflow menu
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            UserProfileButton(
                activeUser = activeUser,
                onSwitchUser = onSwitchUser,
                onEditProfile = onEditProfile,
                onCreateNewUser = onCreateNewUser
            )

            Box {
                IconButton(onClick = { overflowMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.desc_more_options)
                    )
                }
                DropdownMenu(
                    expanded = overflowMenuExpanded,
                    onDismissRequest = { overflowMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_history)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            onOpenHistory()
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (!isRunning) Arrangement.Center else Arrangement.Top
        ) {
            if (isRunning) {
                TimeProgressIndicator(
                    elapsedTime = elapsedTime,
                    targetSeconds = TARGET_TRAINING_SECONDS,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TrainingChart(
                    historyPoints = historyPoints,
                    elapsedTime = elapsedTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isRunning) {
                TopDashboard(trainingData = trainingData)
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalFloatingToolbar(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (!isRunning) {
                    FilledIconButton(
                        onClick = onStart,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.desc_start_training),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = onPauseToggle,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) stringResource(R.string.desc_resume_training) else stringResource(R.string.desc_pause_training),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onStop,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.desc_stop_training),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeProgressIndicator(
    elapsedTime: Long,
    targetSeconds: Long,
    modifier: Modifier = Modifier
) {
    val progress = (elapsedTime.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(150.dp),
            strokeWidth = 10.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.label_time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Chronometer.format(elapsedTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.target_min_format, targetSeconds / 60),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun HorizontalFloatingToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun TopDashboard(
    trainingData: TrainingData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricCard(
            label = stringResource(R.string.label_rpm),
            value = trainingData.rpm.toString(),
            unit = "rpm",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        MetricCard(
            label = stringResource(R.string.label_heart_rate),
            value = if (trainingData.heartRate > 0) trainingData.heartRate.toString() else "--",
            unit = "bpm",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        MetricCard(
            label = stringResource(R.string.label_speed),
            value = trainingData.speed.toString(),
            unit = "km/h",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        MetricCard(
            label = stringResource(R.string.label_distance),
            value = trainingData.distance.toString(),
            unit = "m",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        MetricCard(
            label = stringResource(R.string.label_power),
            value = trainingData.power.toString(),
            unit = "W",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        MetricCard(
            label = stringResource(R.string.label_calories),
            value = trainingData.calories.roundToInt().toString(),
            unit = "kcal",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
        )
        val resistanceColor = getResistanceColor(trainingData.resistance)
        MetricCard(
            label = stringResource(R.string.label_resistance),
            value = when (trainingData.resistance) {
                1 -> stringResource(R.string.val_min)
                10 -> stringResource(R.string.val_max)
                else -> trainingData.resistance.toString()
            },
            unit = "",
            valueColor = resistanceColor,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 3.dp)
        )
    }
}

fun getResistanceColor(resistance: Int): Color {
    val clamped = resistance.coerceIn(1, 10)
    val fraction = (clamped - 1).toFloat() / 9f
    return when {
        fraction < 0.5f -> lerp(ResistanceLowColor, ResistanceMediumColor, fraction * 2f)
        else -> lerp(ResistanceMediumColor, ResistanceHighColor, (fraction - 0.5f) * 2f)
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = unit.ifBlank { " " },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Preview(
    name = "Entrenamiento En Marcha",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun TrackingControlScreenRunningPreview() {
    val mockHistory = remember {
        listOf(
            TrainingHistoryPoint(Date(), 0L, TrainingData(
                rpm = 70,
                resistance = 4,
                heartRate = 120,
            )),
            TrainingHistoryPoint(Date(), 2L, TrainingData(
                rpm = 75,
                resistance = 5,
                heartRate = 125,
            )),
            TrainingHistoryPoint(Date(), 4L, TrainingData(
                rpm = 80,
                resistance = 5,
                heartRate = 130,
            )),
            TrainingHistoryPoint(Date(), 6L, TrainingData(
                rpm = 85,
                resistance = 6,
                heartRate = 135,
            ))
        )
    }
    val sampleUser = UserEntity(id = 1, name = "Alex", avatarId = "face_teal")
    FreeVolabaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TrackingControlContent(
                activeUser = sampleUser,
                isRunning = true,
                isPaused = false,
                trainingData = TrainingData(
                    rpm = 85,
                    resistance = 10,
                    distance = 1200,
                    speed = 28,
                    heartRate = 135,
                ),
                elapsedTime = 225L, // 03:45
                historyPoints = mockHistory,
                onStart = {},
                onPauseToggle = {},
                onStop = {},
                onSwitchUser = {},
                onEditProfile = {},
                onCreateNewUser = {},
                onOpenHistory = {}
            )
        }
    }
}

@Preview(
    name = "Entrenamiento Detenido",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun TrackingControlScreenPreview() {
    val sampleUser = UserEntity(id = 1, name = "Alex", avatarId = "face_teal")
    FreeVolabaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TrackingControlContent(
                activeUser = sampleUser,
                isRunning = false,
                isPaused = false,
                trainingData = TrainingData(),
                elapsedTime = 0L,
                historyPoints = emptyList(),
                onStart = {},
                onPauseToggle = {},
                onStop = {},
                onSwitchUser = {},
                onEditProfile = {},
                onCreateNewUser = {},
                onOpenHistory = {}
            )
        }
    }
}
