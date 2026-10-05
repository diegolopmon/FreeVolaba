package com.diegolopmon.freevolaba.training

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class Chronometer(
    private val scope: CoroutineScope,
    private val tickMillis: Long = 250L
) {
    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()
    private val _elapsedTimeMillis = MutableStateFlow(0L)
    val elapsedTimeMillis: StateFlow<Long> = _elapsedTimeMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var accumulated: Duration = ZERO
    private var mark: TimeSource.Monotonic.ValueTimeMark? = null
    private var tickerJob: Job? = null

    @Synchronized
    fun start() {
        if (mark != null) return
        mark = TimeSource.Monotonic.markNow()
        _isRunning.value = true
        _isPaused.value = false
        startTicker()
    }

    @Synchronized
    fun pause() {
        val current = mark ?: return
        accumulated += current.elapsedNow()
        mark = null
        tickerJob?.cancel()
        tickerJob = null
        val elapsed = currentElapsed()
        _elapsedTimeSeconds.value = elapsed.inWholeSeconds
        _elapsedTimeMillis.value = elapsed.inWholeMilliseconds
        _isRunning.value = false
        _isPaused.value = true
    }

    @Synchronized
    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        mark = null
        accumulated = ZERO
        _elapsedTimeSeconds.value = 0L
        _elapsedTimeMillis.value = 0L
        _isRunning.value = false
        _isPaused.value = false
    }

    private fun startTicker() {
        tickerJob = scope.launch {
            while (isActive) {
                val elapsed = currentElapsed()
                _elapsedTimeSeconds.value = elapsed.inWholeSeconds
                _elapsedTimeMillis.value = elapsed.inWholeMilliseconds
                delay(tickMillis.milliseconds)
            }
        }
    }

    @Synchronized
    private fun currentElapsed(): Duration =
        accumulated + (mark?.elapsedNow() ?: ZERO)

    companion object {
        fun format(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return when {
                hours > 0 -> String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                minutes > 0 -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                seconds > 9 -> String.format(Locale.getDefault(), "%02d", seconds)
                else -> String.format(Locale.getDefault(), "%01d", seconds)
            }
        }
    }
}
