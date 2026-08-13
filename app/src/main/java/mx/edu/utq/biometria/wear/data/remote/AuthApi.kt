package mx.edu.utq.biometria.wear.data.remote

import mx.edu.utq.biometria.wear.data.remote.dto.LoginPinRequestDto
import mx.edu.utq.biometria.wear.data.remote.dto.SessionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    // Response<T> (no un suspend directo devolviendo el body) porque necesitamos leer el header
    // Set-Cookie de la respuesta cruda para extraer el JWT -- el body nunca lo trae.
    @POST("api/auth/login-pin")
    suspend fun loginPin(@Body body: LoginPinRequestDto): Response<SessionResponseDto>

    // Best-effort: con JWT stateless no hay sesion de servidor que invalidar de verdad; se llama
    // por simetria con el resto del ecosistema, sin que su resultado bloquee el logout local.
    // El token se pasa explicito (no via AuthInterceptor) porque el logout local -- que borra el
    // token del AuthTokenStore -- ya corrio para cuando esta llamada sale; si dependiera del
    // interceptor, saldria sin Authorization y el backend la rechazaria.
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>
}
