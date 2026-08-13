package mx.edu.utq.biometria.wear.data.telemetry

import mx.edu.utq.biometria.wear.data.remote.dto.TelemetryIngestRequestDto
import mx.edu.utq.biometria.wear.data.remote.dto.TelemetryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TelemetryApi {

    @POST("api/telemetry/ingest")
    suspend fun ingest(@Body body: TelemetryIngestRequestDto): Response<TelemetryResponseDto>
}
