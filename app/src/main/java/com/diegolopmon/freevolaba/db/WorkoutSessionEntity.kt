package com.diegolopmon.freevolaba.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
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
