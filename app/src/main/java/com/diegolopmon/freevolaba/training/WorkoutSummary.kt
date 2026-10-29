package com.diegolopmon.freevolaba.training

import com.diegolopmon.freevolaba.db.WorkoutSessionEntity
import kotlin.math.roundToInt

data class WorkoutSummary(
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val totalDistance: Int = 0,
    val totalCalories: Int = 0,
    val avgPower: Int = 0,
    val maxPower: Int = 0,
    val avgRpm: Int = 0,
    val maxRpm: Int = 0,
    val avgResistance: Double = 1.0,
    val historyPoints: List<TrainingHistoryPoint> = emptyList()
) {
    fun toEntity(): WorkoutSessionEntity {
        return WorkoutSessionEntity(
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            totalDistance = totalDistance,
            totalCalories = totalCalories,
            avgPower = avgPower,
            maxPower = maxPower,
            avgRpm = avgRpm,
            maxRpm = maxRpm,
            avgResistance = avgResistance,
            historyPoints = historyPoints
        )
    }

    companion object {
        fun calculate(
            historyPoints: List<TrainingHistoryPoint>,
            durationSeconds: Long,
            finalTrainingData: TrainingData? = null
        ): WorkoutSummary {
            if (historyPoints.isEmpty()) {
                val data = finalTrainingData ?: TrainingData()
                return WorkoutSummary(
                    durationSeconds = durationSeconds,
                    totalDistance = data.distance,
                    totalCalories = data.calories.roundToInt(),
                    avgPower = data.power,
                    maxPower = data.power,
                    avgRpm = data.rpm,
                    maxRpm = data.rpm,
                    avgResistance = data.resistance.toDouble(),
                    historyPoints = emptyList()
                )
            }

            val lastData = finalTrainingData ?: historyPoints.last().trainingData
            val powers = historyPoints.map { it.trainingData.power }
            val rpms = historyPoints.map { it.trainingData.rpm }
            val resistances = historyPoints.map { it.trainingData.resistance }

            val avgPower = powers.average().roundToInt()
            val maxPower = powers.maxOrNull() ?: 0
            val avgRpm = rpms.average().roundToInt()
            val maxRpm = rpms.maxOrNull() ?: 0
            val avgResistance = (resistances.average() * 10.0).roundToInt() / 10.0

            return WorkoutSummary(
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                totalDistance = lastData.distance,
                totalCalories = lastData.calories.roundToInt(),
                avgPower = avgPower,
                maxPower = maxPower,
                avgRpm = avgRpm,
                maxRpm = maxRpm,
                avgResistance = avgResistance,
                historyPoints = historyPoints
            )
        }
    }
}
