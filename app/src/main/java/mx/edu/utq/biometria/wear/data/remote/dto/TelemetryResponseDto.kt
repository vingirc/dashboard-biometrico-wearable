package mx.edu.utq.biometria.wear.data.remote.dto

import kotlinx.serialization.Serializable

// Espeja com.biometria.telemetria_api.dto.TelemetryResponse del backend. "timestamp" viaja como
// String ISO-8601 (asi lo serializa Instant en el backend) -- se parsea con java.time.Instant.parse
// solo donde haga falta, sin agregar kotlinx-datetime como dependencia nueva. "isCritical" es la
// fuente de verdad autoritativa (heartRate > 160, calculado en el servidor) -- el reloj nunca la
// recalcula por su cuenta.
@Serializable
data class TelemetryResponseDto(
    val id: String,
    val username: String,
    val heartRate: Int,
    val timestamp: String,
    val isCritical: Boolean,
    val isLow: Boolean = false,
    val accelerometer: List<Double>? = null,
)
