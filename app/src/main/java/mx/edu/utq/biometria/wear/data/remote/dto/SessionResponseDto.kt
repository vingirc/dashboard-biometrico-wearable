package mx.edu.utq.biometria.wear.data.remote.dto

import kotlinx.serialization.Serializable

// Espeja com.biometria.telemetria_api.dto.SessionResponse del backend. El JWT NUNCA viaja aca --
// solo en el header Set-Cookie de la respuesta (ver AuthRepository.extractAuthToken).
@Serializable
data class SessionResponseDto(
    val username: String,
    val role: String,
)
