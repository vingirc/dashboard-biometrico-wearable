package mx.edu.utq.biometria.wear.data.remote

import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response

// El backend acepta el JWT via header Authorization ademas de via cookie -- ese camino es el que
// usa este cliente nativo (nunca usamos un CookieJar, ver NetworkModule).
class AuthInterceptor(private val tokenStore: AuthTokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.getToken()

        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
