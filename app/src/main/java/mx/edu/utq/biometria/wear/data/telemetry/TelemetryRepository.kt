package mx.edu.utq.biometria.wear.data.telemetry

import java.io.IOException
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import mx.edu.utq.biometria.wear.data.remote.dto.TelemetryIngestRequestDto

class TelemetryRepository(
    private val api: TelemetryApi,
    private val tokenStore: AuthTokenStore,
) {

    suspend fun sendReading(heartRate: Int, accelerometer: List<Double>?): TelemetryResult {
        val username = tokenStore.getUsername() ?: return TelemetryResult.Forbidden
        return try {
            val request = TelemetryIngestRequestDto(
                userId = username,
                heartRate = heartRate,
                accelerometer = accelerometer,
            )
            val response = api.ingest(request)
            when (response.code()) {
                201 -> {
                    val body = response.body()
                    if (body == null) {
                        TelemetryResult.NetworkError(IllegalStateException("Respuesta 201 sin body"))
                    } else {
                        TelemetryResult.Success(body)
                    }
                }
                403 -> TelemetryResult.Forbidden
                else -> TelemetryResult.NetworkError(IllegalStateException("HTTP inesperado ${response.code()}"))
            }
        } catch (e: IOException) {
            TelemetryResult.NetworkError(e)
        }
    }
}
