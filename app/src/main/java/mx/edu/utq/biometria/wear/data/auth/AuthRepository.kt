package mx.edu.utq.biometria.wear.data.auth

import java.io.IOException
import mx.edu.utq.biometria.wear.data.remote.AuthApi
import mx.edu.utq.biometria.wear.data.remote.dto.LoginPinRequestDto
import okhttp3.Headers

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
) {

    suspend fun loginPin(username: String, pin: String): AuthResult {
        return try {
            val response = api.loginPin(LoginPinRequestDto(username, pin))
            when (response.code()) {
                200 -> {
                    val token = extractAuthToken(response.headers())
                    if (token == null) {
                        AuthResult.NetworkError(IllegalStateException("Falta la cookie auth_token en la respuesta"))
                    } else {
                        tokenStore.saveToken(token)
                        tokenStore.saveUsername(username)
                        AuthResult.Success
                    }
                }
                401 -> AuthResult.InvalidCredentials
                429 -> AuthResult.RateLimited
                else -> AuthResult.NetworkError(IllegalStateException("HTTP inesperado ${response.code()}"))
            }
        } catch (e: IOException) {
            AuthResult.NetworkError(e)
        }
    }

    // El body de /login-pin nunca trae el JWT (diseno del backend para el navegador); el header
    // Set-Cookie si es legible por un cliente HTTP nativo -- HttpOnly solo restringe JS de
    // navegador, no un OkHttp/Retrofit corriendo en Android. Se extrae el valor a mano, sin usar
    // un CookieJar (ver NetworkModule).
    private fun extractAuthToken(headers: Headers): String? {
        return headers.values("Set-Cookie")
            .firstOrNull { it.startsWith("$AUTH_COOKIE_NAME=") }
            ?.substringAfter("$AUTH_COOKIE_NAME=")
            ?.substringBefore(";")
            ?.takeIf { it.isNotBlank() }
    }

    // Best-effort: no bloquea ni le importa el resultado (logout local ya paso antes de llamar esto).
    // Recibe el token explicito porque el AuthTokenStore ya esta vacio a esta altura.
    suspend fun logoutBestEffort(token: String) {
        try {
            api.logout("Bearer $token")
        } catch (_: IOException) {
            // sin sesion de servidor real que invalidar (JWT stateless); un fallo de red aca es inofensivo
        }
    }

    private companion object {
        // Espeja AuthController.COOKIE_NAME del backend.
        const val AUTH_COOKIE_NAME = "auth_token"
    }
}
