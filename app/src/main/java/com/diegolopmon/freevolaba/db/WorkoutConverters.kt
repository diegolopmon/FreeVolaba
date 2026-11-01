package com.diegolopmon.freevolaba.db

import androidx.room.TypeConverter
import com.diegolopmon.freevolaba.training.TrainingData
import com.diegolopmon.freevolaba.training.TrainingHistoryPoint
import java.util.Date

class WorkoutConverters {

    @TypeConverter
    fun fromHistoryPointsList(points: List<TrainingHistoryPoint>): String {
        if (points.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in points.indices) {
            val p = points[i]
            val d = p.trainingData
            sb.append(p.timestamp.time).append(",")
                .append(p.elapsedTimeSeconds).append(",")
                .append(d.rpm).append(",")
                .append(d.resistance).append(",")
                .append(d.distance).append(",")
                .append(d.speed).append(",")
                .append(d.heartRate).append(",")
                .append(d.calories).append(",")
                .append(d.power)
            if (i < points.size - 1) sb.append(";")
        }
        return sb.toString()
    }

    @TypeConverter
    fun toHistoryPointsList(dataString: String?): List<TrainingHistoryPoint> {
        if (dataString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<TrainingHistoryPoint>()
        try {
            val pointStrings = dataString.split(";")
            for (pStr in pointStrings) {
                if (pStr.isBlank()) continue
                val parts = pStr.split(",")
                if (parts.size >= 9) {
                    val ts = parts[0].toLongOrNull() ?: System.currentTimeMillis()
                    val elapsed = parts[1].toLongOrNull() ?: 0L
                    val rpm = parts[2].toIntOrNull() ?: 0
                    val resistance = parts[3].toIntOrNull() ?: 1
                    val distance = parts[4].toIntOrNull() ?: 0
                    val speed = parts[5].toIntOrNull() ?: 0
                    val hr = parts[6].toIntOrNull() ?: 0
                    val calories = parts[7].toDoubleOrNull() ?: 0.0
                    val power = parts[8].toIntOrNull() ?: 0

                    list.add(
                        TrainingHistoryPoint(
                            timestamp = Date(ts),
                            elapsedTimeSeconds = elapsed,
                            trainingData = TrainingData(
                                rpm = rpm,
                                resistance = resistance,
                                distance = distance,
                                speed = speed,
                                heartRate = hr,
                                calories = calories,
                                power = power
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
