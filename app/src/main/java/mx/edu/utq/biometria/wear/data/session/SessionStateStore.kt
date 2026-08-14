package mx.edu.utq.biometria.wear.data.session

import android.content.Context

// SharedPreferences en claro (a diferencia de AuthTokenStore): esto no es un secreto, solo un
// flag de UI que recuerda si la sesion de envio quedaba "Iniciada" al cerrar la app. Sin esto,
// reabrir la app siempre arrancaba en "Pausada" en silencio, aunque el usuario nunca la hubiera
// pausado a mano.
class SessionStateStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    fun saveActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIVE, active).apply()
    }

    fun wasActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    private companion object {
        const val PREFS_FILE_NAME = "session_state_prefs"
        const val KEY_ACTIVE = "session_active"
    }
}
