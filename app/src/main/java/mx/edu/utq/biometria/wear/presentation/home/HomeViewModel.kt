package mx.edu.utq.biometria.wear.presentation.home

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.edu.utq.biometria.wear.BiometriaWearApplication
import mx.edu.utq.biometria.wear.data.auth.AuthRepository
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import mx.edu.utq.biometria.wear.data.notifications.AlertNotificationManager
import mx.edu.utq.biometria.wear.data.sensors.HapticAlertManager
import mx.edu.utq.biometria.wear.data.sensors.SensorTelemetryManager
import mx.edu.utq.biometria.wear.data.session.SessionStateStore
import mx.edu.utq.biometria.wear.data.telemetry.TelemetryRepository
import mx.edu.utq.biometria.wear.data.telemetry.TelemetryResult
import mx.edu.utq.biometria.wear.data.ws.TelemetryWsClient

private const val SEND_INTERVAL_MS = 5000L
private const val MIN_FAILURES_TO_SHOW_ERROR = 2

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val authTokenStore: AuthTokenStore,
    private val sensorTelemetryManager: SensorTelemetryManager,
    private val telemetryRepository: TelemetryRepository,
    private val telemetryWsClient: TelemetryWsClient,
    private val hapticAlertManager: HapticAlertManager,
    private val alertNotificationManager: AlertNotificationManager,
    private val sessionStateStore: SessionStateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var sendJob: Job? = null
    private var consecutiveSendFailures = 0

    init {
        // El socket se mantiene conectado mientras haya sesion de login activa, sin importar el
        // toggle Iniciada/Pausada -- se desconecta solo al hacer logout (ver logout()).
        authTokenStore.getUsername()?.let { telemetryWsClient.connect(it) }

        viewModelScope.launch {
            sensorTelemetryManager.heartRate.collect { reading ->
                _uiState.update { state ->
                    state.copy(
                        bpm = reading?.bpm,
                        bpmAccuracyLow = reading != null &&
                            reading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                    )
                }
            }
        }

        viewModelScope.launch {
            telemetryWsClient.criticalEvents.collect { event ->
                val kind = when {
                    event.isCritical -> AlertKind.HIGH
                    event.isLow -> AlertKind.LOW
                    else -> null
                }
                if (kind != null) {
                    _uiState.update { it.copy(activeAlert = ActiveAlert(event.heartRate, kind)) }
                    hapticAlertManager.vibrateAlert()
                    alertNotificationManager.postAlert(event.heartRate, kind)
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(permissionState = if (granted) PermissionState.GRANTED else PermissionState.DENIED)
        }
        // Si la sesion habia quedado "Iniciada" cuando se cerro la app la ultima vez, se retoma
        // sola apenas se confirma el permiso de sensor -- si no, la UI mostraria "Iniciada" pero
        // sin mandar nada de verdad hasta que el usuario toque pausa/play a mano (justo lo que
        // pasaba antes: cada apertura arrancaba en Pausada en silencio).
        if (granted && sessionStateStore.wasActive() && _uiState.value.sessionState != SessionState.INICIADA) {
            startSession()
        }
    }

    fun requestPermissionAgain() {
        _uiState.update { it.copy(permissionState = PermissionState.NOT_REQUESTED) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                notificationPermissionState = if (granted) PermissionState.GRANTED else PermissionState.DENIED,
            )
        }
    }

    fun requestNotificationPermissionAgain() {
        _uiState.update { it.copy(notificationPermissionState = PermissionState.NOT_REQUESTED) }
    }

    fun toggleSession() {
        when (_uiState.value.sessionState) {
            SessionState.PAUSADA -> {
                if (_uiState.value.permissionState == PermissionState.GRANTED) startSession()
            }
            SessionState.INICIADA -> stopSession()
        }
    }

    private fun startSession() {
        // El WS no se reconecta solo si se cae (ver TelemetryWsClient) -- backgrounding/modo
        // ambiente prolongado puede matar el socket sin que nada lo note. connect() ya es
        // idempotente (no-op si sigue conectado), asi que reintentar aca es gratis y asegura que,
        // justo cuando mas importa (arrancando una sesion), el canal de alertas este vivo.
        authTokenStore.getUsername()?.let { telemetryWsClient.connect(it) }
        sensorTelemetryManager.start()
        consecutiveSendFailures = 0
        sessionStateStore.saveActive(true)
        _uiState.update { it.copy(sessionState = SessionState.INICIADA, sendError = null) }
        sendJob = viewModelScope.launch {
            while (isActive) {
                // Un fallo (de red o inesperado) en un intento NUNCA debe matar este loop -- si no,
                // el usuario queda con el envio muerto hasta que reabra la app, aunque el error
                // haya sido transitorio (justo lo que se vio en pruebas reales: un primer envio
                // fallido dejaba de reintentar hasta forzar un reinicio manual).
                try {
                    sendCurrentReading()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    recordSendFailure()
                }
                delay(SEND_INTERVAL_MS)
            }
        }
    }

    private fun stopSession() {
        sendJob?.cancel()
        sendJob = null
        sensorTelemetryManager.stop()
        sessionStateStore.saveActive(false)
        _uiState.update { it.copy(sessionState = SessionState.PAUSADA) }
    }

    private suspend fun sendCurrentReading() {
        // 0 no es un latido real -- es lo que reporta el sensor (real o virtual del emulador)
        // cuando todavia no hay una lectura valida. Mandarlo igual ensucia el historial con datos
        // imposibles y el frontend lo mostraria como "Normal" (0 < 160) en vez de "sin datos".
        val bpm = sensorTelemetryManager.heartRate.value?.bpm?.takeIf { it > 0 } ?: return
        val accel = sensorTelemetryManager.accelerometer.value
        val accelList = accel?.let { listOf(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }

        when (val result = telemetryRepository.sendReading(bpm, accelList)) {
            is TelemetryResult.Success -> {
                val timestamp = runCatching { Instant.parse(result.response.timestamp) }
                    .getOrDefault(Instant.now())
                consecutiveSendFailures = 0
                _uiState.update { it.copy(lastReadingAt = timestamp, sendError = null) }
            }
            // No es transitorio -- no tiene sentido esperar a que se repita para avisar.
            TelemetryResult.Forbidden -> _uiState.update {
                it.copy(sendError = "No autorizado para enviar datos")
            }
            is TelemetryResult.NetworkError -> recordSendFailure()
        }
    }

    // Un solo intento fallido (de red, sea IOException o algo inesperado) casi siempre es un
    // bache transitorio -- backgrounding/modo ambiente puede cortar la conexion un instante y el
    // intento de 5s despues suele funcionar solo. Mostrar "Error de conexión" en cada bache asi
    // hacia parecer que la app estaba rota cuando en realidad se autocuraba enseguida. Recien se
    // avisa si el fallo se repite.
    private fun recordSendFailure() {
        consecutiveSendFailures++
        if (consecutiveSendFailures >= MIN_FAILURES_TO_SHOW_ERROR) {
            _uiState.update { it.copy(sendError = "Error de conexión") }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(activeAlert = null) }
    }

    // Para sensores y WS antes de navegar -- si no, una sesion cerrada podria seguir mandando
    // datos o con el socket abierto de fondo. El token se captura ANTES de limpiarlo del store,
    // mismo motivo que en HomeViewModel de Fase 1: el AuthInterceptor ya no tendria nada que
    // adjuntar si se leyera despues.
    fun logout(onLoggedOut: () -> Unit) {
        sendJob?.cancel()
        sensorTelemetryManager.stop()
        telemetryWsClient.disconnect()
        // Sin esto, un proximo login (misma cuenta u otra) resumiria mandando datos solo, sin que
        // nadie haya tocado Iniciar -- confuso sobre todo si es una cuenta distinta.
        sessionStateStore.saveActive(false)

        val token = authTokenStore.getToken()
        authTokenStore.clearToken()
        onLoggedOut()
        if (token != null) {
            viewModelScope.launch {
                authRepository.logoutBestEffort(token)
            }
        }
    }

    companion object {
        fun factory(app: BiometriaWearApplication) = viewModelFactory {
            initializer {
                HomeViewModel(
                    authRepository = app.authRepository,
                    authTokenStore = app.authTokenStore,
                    sensorTelemetryManager = app.sensorTelemetryManager,
                    telemetryRepository = app.telemetryRepository,
                    telemetryWsClient = app.telemetryWsClient,
                    hapticAlertManager = app.hapticAlertManager,
                    alertNotificationManager = app.alertNotificationManager,
                    sessionStateStore = app.sessionStateStore,
                )
            }
        }
    }
}
