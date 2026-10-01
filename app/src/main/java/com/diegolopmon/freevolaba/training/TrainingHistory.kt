package com.diegolopmon.freevolaba.training

import java.util.Date
class TrainingSessionHistory {
    private val _points = mutableListOf<TrainingHistoryPoint>()
    val points: List<TrainingHistoryPoint> get() = _points

    var startTime: Date? = null
        private set

    fun startSession() {
        _points.clear()
        startTime = Date()
    }

    fun addPoint(elapsedTimeSeconds: Long, trainingData: TrainingData, sampleIntervalSeconds: Long = 2L) {
        val lastPoint = _points.lastOrNull()
        if (lastPoint == null || elapsedTimeSeconds - lastPoint.elapsedTimeSeconds >= sampleIntervalSeconds) {
            _points.add(
                TrainingHistoryPoint(
                    timestamp = Date(),
                    elapsedTimeSeconds = elapsedTimeSeconds,
                    trainingData = trainingData
                )
            )
        }
    }

    fun clear() {
        _points.clear()
        startTime = null
    }
}
