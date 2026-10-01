package com.diegolopmon.freevolaba.training

import java.util.Date

data class TrainingHistoryPoint(
    val timestamp: Date,
    val elapsedTimeSeconds: Long,
    val trainingData: TrainingData
)
