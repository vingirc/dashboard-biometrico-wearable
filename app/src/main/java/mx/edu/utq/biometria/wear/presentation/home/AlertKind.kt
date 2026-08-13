package mx.edu.utq.biometria.wear.presentation.home

enum class AlertKind {
    HIGH,
    LOW,
}

// Se guarda el bpm que disparo la alerta aparte del "bpm" en vivo del dashboard: para cuando la
// alerta llega a renderizarse, el sensor pudo haber avanzado a otra lectura -- la alerta siempre
// debe mostrar el valor que la disparo, no lo que el sensor diga en ese instante.
data class ActiveAlert(val bpm: Int, val kind: AlertKind)
