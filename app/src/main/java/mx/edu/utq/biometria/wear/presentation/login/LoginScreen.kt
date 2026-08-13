package mx.edu.utq.biometria.wear.presentation.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.RemoteInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.input.RemoteInputIntentHelper
import mx.edu.utq.biometria.wear.BiometriaWearApplication

private const val EXTRA_USERNAME = "username_input"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(
            LocalContext.current.applicationContext as BiometriaWearApplication,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val cachedUsername by viewModel.cachedUsername.collectAsState()
    var pin by rememberSaveable { mutableStateOf("") }

    val usernameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val username = result.data
                ?.let { RemoteInput.getResultsFromIntent(it) }
                ?.getCharSequence(EXTRA_USERNAME)
                ?.toString()
                ?.trim()
            if (!username.isNullOrBlank()) {
                viewModel.onUsernameCaptured(username)
            }
        }
    }

    fun launchUsernameCapture() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val remoteInputs = listOf(
            RemoteInput.Builder(EXTRA_USERNAME)
                .setLabel("Usuario")
                .build(),
        )
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        usernameLauncher.launch(intent)
    }

    // Si no hay username cacheado (primera vez, o tras "cambiar cuenta"), pedirlo antes que nada.
    LaunchedEffect(cachedUsername) {
        if (cachedUsername == null) {
            launchUsernameCapture()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            if (cachedUsername != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = cachedUsername!!,
                        style = MaterialTheme.typography.caption2,
                    )
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = "Cambiar cuenta",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.changeAccount()
                                pin = ""
                            },
                    )
                }

                Text(text = buildPinIndicator(pin))

                PinKeypad(
                    pinLength = pin.length,
                    confirmEnabled = pin.length in 4..PinMaxLength && uiState != LoginUiState.Loading,
                    onDigit = { digit ->
                        if (pin.length < PinMaxLength) {
                            pin += digit
                            viewModel.resetError()
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            viewModel.resetError()
                        }
                    },
                    onConfirm = { viewModel.submitPin(pin) },
                )

                val state = uiState
                if (state is LoginUiState.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption3,
                    )
                    // El PIN se limpia despues de un intento fallido, no hace falta que el usuario lo borre a mano.
                    LaunchedEffect(state) { pin = "" }
                }
            } else {
                Text("Configurando cuenta…", style = MaterialTheme.typography.caption2)
            }
        }
    }
}
