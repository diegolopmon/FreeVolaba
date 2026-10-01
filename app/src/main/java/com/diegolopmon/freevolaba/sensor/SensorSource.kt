package com.diegolopmon.freevolaba.sensor

interface SensorSource: AutoCloseable {

    fun initialize(onDataReceived: (ByteArray) -> Unit = {}): Boolean
}
