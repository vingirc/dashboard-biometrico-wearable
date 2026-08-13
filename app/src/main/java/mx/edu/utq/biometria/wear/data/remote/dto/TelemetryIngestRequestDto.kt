package mx.edu.utq.biometria.wear.data.remote.dto

import kotlinx.serialization.Serializable

// Espeja com.biometria.telemetria_api.dto.TelemetryIngestRequest del backend. "userId" en
// realidad se compara contra el username -- asi se llama el campo del lado del servidor, no es
// error de nombre de este DTO. accelerometer, si se manda, debe traer EXACTAMENTE 3 valores
// (x,y,z) o el backend responde 400 -- no hay campo de timestamp, lo pone el servidor.
@Serializable
data class TelemetryIngestRequestDto(
    val userId: String,
    val heartRate: Int,
    val accelerometer: List<Double>? = null,
)
