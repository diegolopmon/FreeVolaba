package com.diegolopmon.freevolaba.training

import android.R.drawable.ic_menu_mylocation
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diegolopmon.freevolaba.data.WorkoutRepository
import com.diegolopmon.freevolaba.sensor.SensorData
import com.diegolopmon.freevolaba.sensor.SensorDataDecoder
import com.diegolopmon.freevolaba.sensor.SensorDataRecorder
import com.diegolopmon.freevolaba.sensor.SensorFactory
import com.diegolopmon.freevolaba.sensor.SensorSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TrainingService : Service() {

    private lateinit var sensor: SensorSource
    private val sensorDataDecoder = SensorDataDecoder()
    private lateinit var sensorDataRecorder: SensorDataRecorder
    private val sessionHistory = TrainingSessionHistory()
    private var previousElapsedTimeMillis = 0L

    companion object {
        private const val CHANNEL_ID = "training_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "TrainingService"
        private const val DEADBAND = 600
        private const val VIRTUAL_GEAR_METERS_PER_REV = 3
        private const val ERGOMETER_FLYWHEEL_METERS_PER_REV = 6.0
        private const val KP_PER_LEVEL = 0.3
        private const val KGF_M_MIN_TO_WATTS = 0.1635

        const val ACTION_PAUSE = "com.diegolopmon.freevolaba.action.PAUSE"
        const val ACTION_RESUME = "com.diegolopmon.freevolaba.action.RESUME"

        fun pause(context: Context) {
            val intent = Intent(context, TrainingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TrainingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        private val _currentTrainingData = MutableStateFlow(TrainingData())
        val trainingDataState: StateFlow<TrainingData> = _currentTrainingData.asStateFlow()

        private val _historyState = MutableStateFlow<List<TrainingHistoryPoint>>(emptyList())
        val historyState: StateFlow<List<TrainingHistoryPoint>> = _historyState.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunningState: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastSummaryState = MutableStateFlow<WorkoutSummary?>(null)
        val lastSummaryState: StateFlow<WorkoutSummary?> = _lastSummaryState.asStateFlow()

        val chronometer = Chronometer(CoroutineScope(SupervisorJob() + Dispatchers.Default))

        fun clearSummary() {
            _lastSummaryState.value = null
        }

        fun stopAndSummarize(context: Context) {
            val duration = chronometer.elapsedTimeSeconds.value
            val history = historyState.value
            val currentData = trainingDataState.value
            val summary = WorkoutSummary.calculate(
                historyPoints = history,
                durationSeconds = duration,
                finalTrainingData = currentData
            )
            _lastSummaryState.value = summary

            val repository = WorkoutRepository(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.saveSession(summary)
                    Log.d(TAG, "Successfully persisted workout summary: $summary")
                } catch (e: Exception) {
                    Log.e(TAG, "Error persisting workout summary", e)
                }
            }

            val intent = Intent(context, TrainingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        sensorDataRecorder = SensorDataRecorder(this)
        sessionHistory.startSession()
        _historyState.value = emptyList()
        previousElapsedTimeMillis = 0L
        sensor = SensorFactory.create(this)
        sensor.initialize(::processSensorData)
        chronometer.start()
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> onPause()
            ACTION_RESUME -> onResume()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensor.close()
        sensorDataRecorder.close()
        _currentTrainingData.value = TrainingData()
        _historyState.value = emptyList()
        chronometer.stop()
        _isRunning.value = false
        sessionHistory.clear()
    }

    fun onPause() {
        chronometer.pause()
    }

    fun onResume() {
        chronometer.start()
        previousElapsedTimeMillis = chronometer.elapsedTimeMillis.value
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Entrenamiento", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entrenamiento en marcha")
            .setContentText("Grabando datos de la bici")
            .setSmallIcon(ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun processSensorData(data: ByteArray) {
        if (chronometer.isPaused.value) return
        val message = data.decodeToString()
        val sensorData = sensorDataDecoder.decodeSensorData(message)
        sensorDataRecorder.record(sensorData)

        if (sensorData != null) {
            val tracking = calculateTrackingData(sensorData, _currentTrainingData.value)
            _currentTrainingData.value = tracking
            sessionHistory.addPoint(chronometer.elapsedTimeSeconds.value, tracking)
            _historyState.value = sessionHistory.points.toList()

            Log.d(
                TAG,
                "Received valid message: ${sensorData.rawMessage} -> RPM: ${tracking.rpm}, Resistance: ${tracking.resistance}, Speed: ${tracking.speed} km/h, Distance: ${tracking.distance} m"
            )
        } else {
            Log.w(TAG, "Received invalid message: $message")
        }
    }

    // Calculation methods
    private fun calculateRPM(rawRPM: Int): Int {
        if (rawRPM < DEADBAND) return 0
        return (rawRPM + 50) / 100
    }

    private fun calculateResistance(rawResistance: Int): Int {
        val minRaw = 500
        val maxRaw = 1800
        val clampedRaw = rawResistance.coerceIn(minRaw, maxRaw)

        val numerator = maxRaw - clampedRaw
        val denominator = maxRaw - minRaw
        val mapped = 1 + (numerator * 9) / denominator

        return mapped.coerceIn(1, 10)
    }

    private fun calculateDeltaDistance(
        rpm: Int,
        currentDistance: Int,
        previousElapsedMillis: Long,
        currentElapsedMillis: Long
    ): Int {
        val deltaMillis = currentElapsedMillis - previousElapsedMillis
        if (deltaMillis <= 0) return currentDistance

        val deltaMeters = rpm * VIRTUAL_GEAR_METERS_PER_REV * (deltaMillis / 60000.0)
        return (currentDistance + deltaMeters).roundToInt()
    }

    private fun calculateCalories(
        powerWatts: Int,
        previousCalories: Double,
        previousElapsedMillis: Long,
        currentElapsedMillis: Long
    ): Double {
        val deltaMillis = currentElapsedMillis - previousElapsedMillis
        if (deltaMillis <= 0 || powerWatts <= 0) return previousCalories

        val metabolicWatts = powerWatts / 0.21
        val deltaKcal = metabolicWatts * (deltaMillis / 1000.0) / 4184.0
        return previousCalories + deltaKcal
    }

    private fun calculateSpeed(rpm: Int): Int {
        return (rpm * VIRTUAL_GEAR_METERS_PER_REV * 0.06).roundToInt()
    }

    private fun calculatePower(rpm: Int, resistanceLevel: Int): Int {
        if (rpm == 0) return 0
        val brakeForceKp = resistanceLevel * KP_PER_LEVEL
        val watts = brakeForceKp * ERGOMETER_FLYWHEEL_METERS_PER_REV * rpm * KGF_M_MIN_TO_WATTS
        return watts.roundToInt()
    }

    private fun calculateTrackingData(
        sensorData: SensorData,
        trainingData: TrainingData
    ): TrainingData {
        val rpm = calculateRPM(sensorData.rpm)
        val resistance = calculateResistance(sensorData.resistance)
        val power = calculatePower(rpm, resistance)
        val currentElapsedMillis = chronometer.elapsedTimeMillis.value

        val newDistance = calculateDeltaDistance(
            rpm,
            trainingData.distance,
            previousElapsedTimeMillis,
            currentElapsedMillis
        )
        val newCalories = calculateCalories(
            power,
            trainingData.calories,
            previousElapsedTimeMillis,
            currentElapsedMillis
        )
        previousElapsedTimeMillis = currentElapsedMillis

        return TrainingData(
            rpm = rpm,
            resistance = resistance,
            distance = newDistance,
            speed = calculateSpeed(rpm),
            heartRate = 0,
            calories = newCalories,
            power = power
        )
    }
}
