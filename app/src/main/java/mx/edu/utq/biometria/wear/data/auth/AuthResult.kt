package mx.edu.utq.biometria.wear.data.auth

sealed interface AuthResult {
    data object Success : AuthResult
    // 401 uniforme del backend: cuenta inexistente, PIN incorrecto, deshabilitada o bloqueada son
    // todas indistinguibles a proposito (diseno anti-enumeracion ya implementado en el backend).
    data object InvalidCredentials : AuthResult
    data object RateLimited : AuthResult
    data class NetworkError(val cause: Throwable) : AuthResult
}
