package mx.edu.utq.biometria.wear.data.ws

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mx.edu.utq.biometria.wear.BuildConfig
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore
import mx.edu.utq.biometria.wear.data.remote.dto.TelemetryResponseDto
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

// 0.toChar(), no un caracter NUL literal embebido en el archivo: un byte NUL crudo en el
// codigo fuente no sobrevive de forma confiable las ediciones/reescrituras de este archivo -- en
// algun punto termino convertido en un espacio comun (0x20) sin que se notara, lo que rompia el
// framing de STOMP (el servidor nunca veia un frame terminado de verdad y jamas contestaba el CONNECT).
private val NUL: Char = 0.toChar()
private const val RECONNECT_DELAY_MS = 5000L

// Cliente STOMP minimo hecho a mano sobre un WebSocket crudo de OkHttp -- no una libreria STOMP
// completa, porque solo hacen falta 2 frames de salida (CONNECT, SUBSCRIBE) y 1 de entrada
// (MESSAGE). El backend expone /ws con SockJS, pero SockJS envuelve cada frame en su propio
// formato de texto; para poder mandar el header nativo "Authorization" en el CONNECT (el handshake
// HTTP de este cliente no lleva cookie, ver JwtHandshakeInterceptor/StompAuthChannelInterceptor)
// hay que conectar al sub-path /websocket que Spring expone automaticamente junto a cualquier
// endpoint .withSockJS(), que sirve WebSocket puro sin el envoltorio SockJS.
//
// Se reconecta solo si el socket se cae mientras deberia seguir conectado (ver shouldStayConnected):
// una primera version de esto NO reconectaba en segundo plano y dejaba la alerta en vivo muerta
// hasta que el usuario tocara Pausar/Iniciar a mano -- esta clase ahora se encarga sola.
class TelemetryWsClient(
    private val okHttpClient: OkHttpClient,
    private val tokenStore: AuthTokenStore,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var pendingUsername: String? = null
    private var shouldStayConnected = false
    private var reconnectJob: Job? = null

    private val _criticalEvents = MutableSharedFlow<TelemetryResponseDto>(replay = 0, extraBufferCapacity = 1)
    val criticalEvents: SharedFlow<TelemetryResponseDto> = _criticalEvents.asSharedFlow()

    fun connect(username: String) {
        shouldStayConnected = true
        pendingUsername = username
        reconnectJob?.cancel()
        openSocket()
    }

    // Siempre reconecta (no reusa una conexion "viva" a medias): sin esto, un socket zombie
    // (webSocket != null pero ya inservible, sin que onFailure/onClosed avisen nunca si nadie
    // manda pings) podia quedar "conectado" para siempre sin funcionar de verdad.
    private fun openSocket() {
        webSocket?.close(1000, null)
        webSocket = null

        val token = tokenStore.getToken() ?: return
        if (pendingUsername == null) return

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}/ws/websocket")
            .build()

        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(connectFrame(token))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleFrame(webSocket, text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w("TelemetryWs", "WebSocket fallo: ${t.message}")
                    this@TelemetryWsClient.webSocket = null
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    this@TelemetryWsClient.webSocket = null
                    scheduleReconnect()
                }
            },
        )
    }

    // Reintenta con un delay fijo (no backoff exponencial: el backend es local/de la LAN, no hace
    // falta esa complejidad aca) mientras la sesion siga considerando que deberia estar conectada.
    // disconnect() (logout) es lo unico que apaga esto.
    private fun scheduleReconnect() {
        if (!shouldStayConnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (shouldStayConnected) openSocket()
        }
    }

    fun disconnect() {
        shouldStayConnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.send(disconnectFrame())
        webSocket?.close(1000, null)
        webSocket = null
        pendingUsername = null
    }

    private fun handleFrame(webSocket: WebSocket, raw: String) {
        val (command, body) = parseFrame(raw)
        when (command) {
            "CONNECTED" -> pendingUsername?.let { webSocket.send(subscribeFrame(it)) }
            "MESSAGE" -> runCatching { json.decodeFromString<TelemetryResponseDto>(body) }
                .onSuccess { _criticalEvents.tryEmit(it) }
                .onFailure { Log.w("TelemetryWs", "No se pudo leer el body de un MESSAGE: ${it.message}") }
            "ERROR" -> Log.w("TelemetryWs", "STOMP ERROR frame: $body")
        }
    }

    private fun parseFrame(raw: String): Pair<String, String> {
        val trimmed = raw.trimEnd(NUL)
        val splitIndex = trimmed.indexOf("\n\n")
        val headerPart = if (splitIndex >= 0) trimmed.substring(0, splitIndex) else trimmed
        val body = if (splitIndex >= 0) trimmed.substring(splitIndex + 2) else ""
        val command = headerPart.lineSequence().firstOrNull().orEmpty()
        return command to body
    }

    private fun connectFrame(token: String) =
        "CONNECT\naccept-version:1.2\nAuthorization:Bearer $token\n\n$NUL"

    private fun subscribeFrame(username: String) =
        "SUBSCRIBE\nid:sub-0\ndestination:/topic/telemetry/$username\n\n$NUL"

    private fun disconnectFrame() = "DISCONNECT\n\n$NUL"
}
