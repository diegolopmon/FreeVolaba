package com.diegolopmon.freevolaba.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val durationSeconds: Long,
    val totalDistance: Int,
    val totalCalories: Int,
    val avgPower: Int,
    val maxPower: Int,
    val avgRpm: Int,
    val maxRpm: Int,
    val avgResistance: Double,
    val historyPoints: List<TrainingHistoryPoint> = emptyList()
)
