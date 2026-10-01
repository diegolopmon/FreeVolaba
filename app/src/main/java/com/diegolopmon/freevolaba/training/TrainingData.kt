package com.diegolopmon.freevolaba.training

data class TrainingData(
    val rpm: Int = 0,
    val resistance: Int = 1,
    val distance: Int = 0,              // meters
    val speed: Int = 0,                 // km/h
    val heartRate: Int = 0,             // BPM
    val calories: Int = 0,              // calories
)
