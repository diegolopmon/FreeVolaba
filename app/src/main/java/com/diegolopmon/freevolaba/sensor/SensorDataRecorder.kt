package com.diegolopmon.freevolaba.sensor

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorDataRecorder(context: Context) {

    companion object {
        private const val TAG = "SensorDataRecorder"
        private const val CSV_HEADER = "Timestamp,RawMessage,RPM,Resistance,Distance\n"
    }

    private var csvFile: File? = null
    private var writer: BufferedWriter? = null

    init {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val fileName = "sensor_data_$timestamp.csv"

            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)
            csvFile = file

            val fileWriter = FileWriter(file, true)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(CSV_HEADER)
            bufferedWriter.flush()
            writer = bufferedWriter

            Log.d(TAG, "Created CSV file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating CSV file", e)
        }
    }

    @Synchronized
    fun record(sensorData: SensorData?) {
        if (sensorData == null) return
        val currentWriter = writer ?: return
        try {
            val recordTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val line = "$recordTimestamp,${sensorData.rawMessage},${sensorData.rpm},${sensorData.resistance},${sensorData.distance}\n"
            currentWriter.write(line)
            currentWriter.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sensor data to CSV", e)
        }
    }

    @Synchronized
    fun close() {
        try {
            writer?.flush()
            writer?.close()
            writer = null
            Log.d(TAG, "CSV writer closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing CSV writer", e)
        }
    }
}
