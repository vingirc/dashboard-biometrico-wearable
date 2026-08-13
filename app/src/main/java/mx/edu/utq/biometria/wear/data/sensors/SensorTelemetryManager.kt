package mx.edu.utq.biometria.wear.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeartRateReading(val bpm: Int, val accuracy: Int)

data class AccelerometerReading(val x: Float, val y: Float, val z: Float)

// Wrapper simple de SensorManager -- sin Compose, sin ViewModel, mismo estilo que AuthRepository.
// start()/stop() son literalmente lo que hace que "Pausada" no gaste sensor/bateria: nada se lee
// mientras no este activo.
class SensorTelemetryManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _heartRate = MutableStateFlow<HeartRateReading?>(null)
    val heartRate: StateFlow<HeartRateReading?> = _heartRate.asStateFlow()

    private val _accelerometer = MutableStateFlow<AccelerometerReading?>(null)
    val accelerometer: StateFlow<AccelerometerReading?> = _accelerometer.asStateFlow()

    fun isHeartRateSensorAvailable(): Boolean = heartRateSensor != null

    private val heartRateListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            _heartRate.value = HeartRateReading(bpm = event.values[0].toInt(), accuracy = event.accuracy)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            _accelerometer.value = AccelerometerReading(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        heartRateSensor?.let {
            sensorManager.registerListener(heartRateListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(accelerometerListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(heartRateListener)
        sensorManager.unregisterListener(accelerometerListener)
        _heartRate.value = null
        _accelerometer.value = null
    }
}
