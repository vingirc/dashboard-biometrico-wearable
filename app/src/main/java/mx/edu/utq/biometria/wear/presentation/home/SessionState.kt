package mx.edu.utq.biometria.wear.presentation.home

// Puramente del lado del reloj -- el backend no tiene ningun concepto de sesion
// iniciada/pausada. El valor en si vive solo en memoria (HomeUiState); si estaba INICIADA se
// persiste por separado en SessionStateStore para retomar sola al reabrir la app (ver
// HomeViewModel.onPermissionResult).
enum class SessionState {
    INICIADA,
    PAUSADA,
}
