package com.diegolopmon.freevolaba.training

import java.util.Date
class TrainingSessionHistory {
    val points: List<TrainingHistoryPoint>
        field = mutableListOf<TrainingHistoryPoint>()

    var startTime: Date? = null
        private set

    fun startSession() {
        points.clear()
        startTime = Date()
    }

    fun addPoint(elapsedTimeSeconds: Long, trainingData: TrainingData, sampleIntervalSeconds: Long = 2L) {
        val lastPoint = points.lastOrNull()
        if (lastPoint == null || elapsedTimeSeconds - lastPoint.elapsedTimeSeconds >= sampleIntervalSeconds) {
            points.add(
                TrainingHistoryPoint(
                    timestamp = Date(),
                    elapsedTimeSeconds = elapsedTimeSeconds,
                    trainingData = trainingData
                )
            )
        }
    }

    fun clear() {
        points.clear()
        startTime = null
    }
}
