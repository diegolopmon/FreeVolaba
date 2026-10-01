package com.diegolopmon.freevolaba.sensor

class SensorDataDecoder {

    private fun isValidMessage(message: String): Boolean {
        return message.startsWith("$") &&
                message.length == 14
    }

    // Raw value getters
    private fun getRawRPM(message: String): Int {
        if (message.length >= 5) {
            return message.takeLast(5).toIntOrNull() ?: 0
        }
        return 0
    }

    private fun getRawResistance(message: String): Int {
        if (message.length >= 9) {
            return message.substring(5, 9).toIntOrNull() ?: 1800
        }
        return 1800
    }

    private fun getRawDistance(message: String): Int {
        if (message.length >= 5) {
            return message.substring(2, 5).toIntOrNull() ?: 0
        }
        return 0
    }

    /**
     * Parses the raw frame message into raw [SensorData].
     */
    fun decodeSensorData(message: String): SensorData? {
        if (!isValidMessage(message)) {
            return null
        }

        return SensorData(
            rawMessage = message,
            rpm = getRawRPM(message),
            resistance = getRawResistance(message),
            distance = getRawDistance(message)
        )
    }
}
