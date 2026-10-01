package com.diegolopmon.freevolaba.sensor

import android.content.Context

object SensorFactory {
    fun create(context: Context): SensorSource {
        return FakeSensor(context)
    }
}
