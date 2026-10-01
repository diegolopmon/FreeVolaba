package com.diegolopmon.freevolaba.sensor

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

class UsbSensor(private val context: Context) : SensorSource {

    companion object {
        private const val TAG = "UsbSensor"
        private const val BAUD_RATE = 19200
        private const val DATA_BITS = 8
    }

    var port: UsbSerialPort? = null
        private set

    private var ioManager: SerialInputOutputManager? = null

    override fun initialize(onDataReceived: (ByteArray) -> Unit): Boolean {
        close()
        Log.d(TAG, "Opening USB connection")
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false
        val driver =
            UsbSerialProber.getDefaultProber().findAllDrivers(manager).firstOrNull() ?: return false
        val connection = manager.openDevice(driver.device) ?: return false
        val usbPort = driver.ports.firstOrNull() ?: return false
        Log.d(TAG, "Connected to USB device: " + usbPort.device.deviceName)

        port = usbPort

        return try {
            usbPort.open(connection)
            usbPort.setParameters(BAUD_RATE, DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            val receiveBuffer = StringBuilder()

            val listener = object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) {
                    val text = data.decodeToString()
                    synchronized(receiveBuffer) {
                        receiveBuffer.append(text)
                        
                        val packetLength = 14 // Each frame is exactly 14 characters long (e.g. $8018053400057)

                        while (receiveBuffer.length >= packetLength) {
                            val dollarIndex = receiveBuffer.indexOf('$')
                            if (dollarIndex == -1) {
                                if (receiveBuffer.length > 1024) receiveBuffer.clear()
                                break
                            }

                            // Discard any junk data before the '$' symbol
                            if (dollarIndex > 0) {
                                receiveBuffer.delete(0, dollarIndex)
                            }

                            // Process if we have the exact frame length
                            if (receiveBuffer.length >= packetLength) {
                                val packet = receiveBuffer.substring(0, packetLength)
                                onDataReceived(packet.encodeToByteArray())
                                receiveBuffer.delete(0, packetLength)
                            } else {
                                break
                            }
                        }
                    }
                }

                override fun onRunError(e: Exception) {
                    e.printStackTrace()
                }
            }

            ioManager = SerialInputOutputManager(usbPort, listener).also {
                it.start()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    override fun close() {
        try {
            ioManager?.stop()
            ioManager = null
            port?.close()
            Log.d(TAG, "Closed USB connection")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            port = null
        }
    }
}
