package mx.edu.utq.biometria.wear.presentation.home

// Puramente del lado del reloj -- el backend no tiene ningun concepto de sesion
// iniciada/pausada. No se persiste: cada apertura de la app arranca en PAUSADA.
enum class SessionState {
    INICIADA,
    PAUSADA,
}
