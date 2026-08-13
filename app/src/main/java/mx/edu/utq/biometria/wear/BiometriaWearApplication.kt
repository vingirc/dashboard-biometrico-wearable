package mx.edu.utq.biometria.wear

import android.app.Application
import mx.edu.utq.biometria.wear.data.auth.AuthRepository
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import mx.edu.utq.biometria.wear.data.notifications.AlertNotificationManager
import mx.edu.utq.biometria.wear.data.remote.NetworkModule
import mx.edu.utq.biometria.wear.data.sensors.HapticAlertManager
import mx.edu.utq.biometria.wear.data.sensors.SensorTelemetryManager
import mx.edu.utq.biometria.wear.data.telemetry.TelemetryRepository
import mx.edu.utq.biometria.wear.data.ws.TelemetryWsClient
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

// Sin Hilt/Koin en esta fase: se arman las dependencias a mano y se exponen desde aca. Suficiente
// para el tamano actual del grafo; se puede introducir DI mas adelante si hace falta.
class BiometriaWearApplication : Application() {

    lateinit var authTokenStore: AuthTokenStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var sensorTelemetryManager: SensorTelemetryManager
        private set

    lateinit var telemetryRepository: TelemetryRepository
        private set

    lateinit var telemetryWsClient: TelemetryWsClient
        private set

    lateinit var hapticAlertManager: HapticAlertManager
        private set

    lateinit var alertNotificationManager: AlertNotificationManager
        private set

    override fun onCreate() {
        super.onCreate()
        authTokenStore = AuthTokenStore(this)
        val authApi = NetworkModule.buildAuthApi(authTokenStore)
        authRepository = AuthRepository(authApi, authTokenStore)

        sensorTelemetryManager = SensorTelemetryManager(this)
        hapticAlertManager = HapticAlertManager(this)
        alertNotificationManager = AlertNotificationManager(this)
        val telemetryApi = NetworkModule.buildTelemetryApi(authTokenStore)
        telemetryRepository = TelemetryRepository(telemetryApi, authTokenStore)
        // Cliente plano, sin AuthInterceptor: la autenticacion del WS viaja dentro del frame STOMP
        // CONNECT (ver TelemetryWsClient), no como header en el handshake HTTP. pingInterval hace
        // que OkHttp mande pings periodicos y note por su cuenta si la conexion murio en silencio
        // (backgrounding/modo ambiente prolongado) -- sin esto, un socket zombie podia quedar
        // "conectado" para siempre sin que nada lo notara.
        val wsHttpClient = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
        telemetryWsClient = TelemetryWsClient(wsHttpClient, authTokenStore)
    }
}
