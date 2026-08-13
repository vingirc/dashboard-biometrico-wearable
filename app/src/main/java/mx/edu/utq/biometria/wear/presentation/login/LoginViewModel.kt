package mx.edu.utq.biometria.wear.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.edu.utq.biometria.wear.BiometriaWearApplication
import mx.edu.utq.biometria.wear.data.auth.AuthRepository
import mx.edu.utq.biometria.wear.data.auth.AuthResult
import mx.edu.utq.biometria.wear.data.auth.AuthTokenStore

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val authTokenStore: AuthTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _cachedUsername = MutableStateFlow(authTokenStore.getUsername())
    val cachedUsername: StateFlow<String?> = _cachedUsername.asStateFlow()

    fun onUsernameCaptured(username: String) {
        _cachedUsername.value = username
    }

    fun changeAccount() {
        authTokenStore.clearUsername()
        _cachedUsername.value = null
    }

    fun submitPin(pin: String) {
        val username = _cachedUsername.value ?: return
        if (_uiState.value == LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _uiState.value = when (val result = authRepository.loginPin(username, pin)) {
                AuthResult.Success -> LoginUiState.Success
                AuthResult.InvalidCredentials -> LoginUiState.Error("Credenciales inválidas")
                AuthResult.RateLimited -> LoginUiState.Error("Demasiados intentos, espera un poco")
                is AuthResult.NetworkError -> LoginUiState.Error("Error de conexión")
            }
        }
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    companion object {
        fun factory(app: BiometriaWearApplication) = viewModelFactory {
            initializer { LoginViewModel(app.authRepository, app.authTokenStore) }
        }
    }
}
