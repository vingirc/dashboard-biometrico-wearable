package mx.edu.utq.biometria.wear.data.telemetry

import mx.edu.utq.biometria.wear.data.remote.dto.TelemetryResponseDto

sealed interface TelemetryResult {
    data class Success(val response: TelemetryResponseDto) : TelemetryResult
    // 403 del backend: la cuenta del token no coincide con userId, o esta deshabilitada -- el
    // backend no distingue el motivo a proposito (TelemetryService.ingest).
    data object Forbidden : TelemetryResult
    data class NetworkError(val cause: Throwable) : TelemetryResult
}
