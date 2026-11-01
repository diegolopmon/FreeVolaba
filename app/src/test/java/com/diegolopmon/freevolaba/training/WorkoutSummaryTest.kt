package com.diegolopmon.freevolaba.training

import com.diegolopmon.freevolaba.db.WorkoutConverters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Date

class WorkoutSummaryTest {

    @Test
    @DisplayName("Empty history points uses final training data or defaults")
    fun testEmptyHistoryPoints() {
        val summary = WorkoutSummary.calculate(
            historyPoints = emptyList(),
            durationSeconds = 120L,
            finalTrainingData = TrainingData(
                rpm = 80,
                resistance = 5,
                distance = 500,
                calories = 35.5,
                power = 150
            )
        )

        assertEquals(120L, summary.durationSeconds)
        assertEquals(500, summary.totalDistance)
        assertEquals(36, summary.totalCalories)
        assertEquals(150, summary.avgPower)
        assertEquals(150, summary.maxPower)
        assertEquals(80, summary.avgRpm)
        assertEquals(80, summary.maxRpm)
        assertEquals(5.0, summary.avgResistance, 0.01)
        assertEquals(0, summary.historyPoints.size)
    }

    @Test
    @DisplayName("Calculate averages and max values from multiple history points")
    fun testMultipleHistoryPointsCalculations() {
        val points = listOf(
            TrainingHistoryPoint(
                timestamp = Date(),
                elapsedTimeSeconds = 10L,
                trainingData = TrainingData(rpm = 60, resistance = 2, distance = 100, calories = 5.0, power = 100)
            ),
            TrainingHistoryPoint(
                timestamp = Date(),
                elapsedTimeSeconds = 20L,
                trainingData = TrainingData(rpm = 80, resistance = 4, distance = 250, calories = 15.0, power = 200)
            ),
            TrainingHistoryPoint(
                timestamp = Date(),
                elapsedTimeSeconds = 30L,
                trainingData = TrainingData(rpm = 100, resistance = 6, distance = 450, calories = 30.4, power = 300)
            )
        )

        val summary = WorkoutSummary.calculate(
            historyPoints = points,
            durationSeconds = 30L
        )

        assertEquals(30L, summary.durationSeconds)
        assertEquals(450, summary.totalDistance)
        assertEquals(30, summary.totalCalories)

        assertEquals(200, summary.avgPower)
        assertEquals(300, summary.maxPower)
        assertEquals(80, summary.avgRpm)
        assertEquals(100, summary.maxRpm)
        assertEquals(4.0, summary.avgResistance, 0.01)
        assertEquals(3, summary.historyPoints.size)
    }

    @Test
    @DisplayName("Entity conversion retains exact summary values")
    fun testToEntityConversion() {
        val summary = WorkoutSummary(
            timestamp = 1600000000000L,
            durationSeconds = 600L,
            totalDistance = 3000,
            totalCalories = 121,
            avgPower = 180,
            maxPower = 250,
            avgRpm = 85,
            maxRpm = 110,
            avgResistance = 4.5
        )

        val entity = summary.toEntity()

        assertEquals(1600000000000L, entity.timestamp)
        assertEquals(600L, entity.durationSeconds)
        assertEquals(3000, entity.totalDistance)
        assertEquals(121, entity.totalCalories)
        assertEquals(180, entity.avgPower)
        assertEquals(250, entity.maxPower)
        assertEquals(85, entity.avgRpm)
        assertEquals(110, entity.maxRpm)
        assertEquals(4.5, entity.avgResistance, 0.01)
    }

    @Test
    @DisplayName("WorkoutConverters serializes and deserializes history points correctly")
    fun testWorkoutConverters() {
        val converter = WorkoutConverters()
        val originalPoints = listOf(
            TrainingHistoryPoint(
                timestamp = Date(1600000000000L),
                elapsedTimeSeconds = 15L,
                trainingData = TrainingData(rpm = 75, resistance = 3, distance = 150, calories = 12.5, power = 130)
            )
        )

        val json = converter.fromHistoryPointsList(originalPoints)
        val deserialized = converter.toHistoryPointsList(json)

        assertEquals(1, deserialized.size)
        assertEquals(15L, deserialized[0].elapsedTimeSeconds)
        assertEquals(75, deserialized[0].trainingData.rpm)
        assertEquals(3, deserialized[0].trainingData.resistance)
        assertEquals(150, deserialized[0].trainingData.distance)
        assertEquals(130, deserialized[0].trainingData.power)
        assertEquals(12.5, deserialized[0].trainingData.calories, 0.01)
    }
}
