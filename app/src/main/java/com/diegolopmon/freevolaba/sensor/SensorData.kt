package com.diegolopmon.freevolaba.sensor

data class SensorData(
    val rawMessage: String,
    val rpm: Int,
    val resistance: Int,
    val distance: Int
)
