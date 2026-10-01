package com.diegolopmon.freevolaba.sensor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.milliseconds

class FakeSensor(
    private val context: Context,
    private val fileName: String = "dummy/sensor_data_20260928_121548_135.csv"
) : SensorSource {

    companion object {
        private const val TAG = "FakeSensor"
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initialize(onDataReceived: (ByteArray) -> Unit): Boolean {
        Log.d(TAG, "Starting FakeSensor with file: $fileName")

        job = scope.launch {
            try {
                val inputStream = context.assets.open(fileName)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                inputStream.close()

                // Split or read by '$' character to trigger events
                val parts = content.split("$")
                for (part in parts) {
                    if (!isActive) break
                    if (part.isNotEmpty()) {
                        val packet = "$" + part
                        Log.d(TAG, "Fake emitting packet: $packet")
                        onDataReceived(packet.toByteArray())
                        delay(150.milliseconds) // Simulation delay between packets
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading mock file", e)
            }
        }
        return true
    }

    override fun close() {
        Log.d(TAG, "Closing FakeSensor")
        job?.cancel()
        job = null
    }
}
