package mx.edu.utq.biometria.wear.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// El JWT (y el username cacheado para no volver a pedirlo cada vez) se guardan cifrados con
// EncryptedSharedPreferences (AES-256, respaldado por Android Keystore) -- nunca en texto plano.
class AuthTokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    // El username no es secreto (no es una credencial), pero se guarda en el mismo archivo
    // cifrado por simplicidad -- así el usuario no tiene que volver a escribirlo con el teclado
    // remoto en cada apertura de la app, solo el PIN.
    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun clearUsername() {
        prefs.edit().remove(KEY_USERNAME).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "auth_secure_prefs"
        const val KEY_TOKEN = "jwt"
        const val KEY_USERNAME = "cached_username"
    }
}
