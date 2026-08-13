package mx.edu.utq.biometria.wear.data.remote.dto

import kotlinx.serialization.Serializable

// Espeja com.biometria.telemetria_api.dto.LoginPinRequest del backend.
@Serializable
data class LoginPinRequestDto(
    val username: String,
    val pin: String,
)
