package mx.edu.utq.biometria.wear.presentation.home

import java.time.Instant

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
}

data class HomeUiState(
    val sessionState: SessionState = SessionState.PAUSADA,
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val notificationPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val bpm: Int? = null,
    val bpmAccuracyLow: Boolean = false,
    val lastReadingAt: Instant? = null,
    val activeAlert: ActiveAlert? = null,
    val sendError: String? = null,
)
