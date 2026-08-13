package mx.edu.utq.biometria.wear.data.remote

import kotlinx.serialization.json.Json
import mx.edu.utq.biometria.wear.BuildConfig
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import mx.edu.utq.biometria.wear.data.telemetry.TelemetryApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    fun buildAuthApi(tokenStore: AuthTokenStore): AuthApi {
        // Deliberadamente SIN CookieJar: nunca queremos que OkHttp administre/reenvie el header
        // Set-Cookie solo -- lo leemos una vez a mano en AuthRepository y de ahi en adelante todo
        // viaja por el header Authorization (ver AuthInterceptor).
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("${BuildConfig.API_BASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(AuthApi::class.java)
    }

    fun buildTelemetryApi(tokenStore: AuthTokenStore): TelemetryApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("${BuildConfig.API_BASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(TelemetryApi::class.java)
    }
}
