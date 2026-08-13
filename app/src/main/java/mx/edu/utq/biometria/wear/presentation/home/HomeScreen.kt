package mx.edu.utq.biometria.wear.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import java.time.Duration
import java.time.Instant
import mx.edu.utq.biometria.wear.BiometriaWearApplication

@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            LocalContext.current.applicationContext as BiometriaWearApplication,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    // Se pide una sola vez al entrar (o cada vez que el usuario toca "reintentar", que resetea
    // el estado a NOT_REQUESTED via requestPermissionAgain()).
    LaunchedEffect(uiState.permissionState) {
        if (uiState.permissionState == PermissionState.NOT_REQUESTED) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS,
            ) == PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                viewModel.onPermissionResult(true)
            } else {
                permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onNotificationPermissionResult(granted) }

    // POST_NOTIFICATIONS solo existe desde API 33 -- en versiones anteriores se reporta concedido
    // de una, sin mostrar dialogo (el permiso ni existe como tal antes de Tiramisu). Si el usuario
    // lo niega no se bloquea nada: la alerta a pantalla completa + la vibracion ya avisan igual,
    // la notificacion del sistema es un canal extra, no el unico.
    LaunchedEffect(uiState.notificationPermissionState) {
        if (uiState.notificationPermissionState == PermissionState.NOT_REQUESTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                viewModel.onNotificationPermissionResult(true)
            } else {
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (alreadyGranted) {
                    viewModel.onNotificationPermissionResult(true)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    Scaffold(timeText = { TimeText() }) {
        val alert = uiState.activeAlert
        if (alert != null) {
            AlertScreen(alert = alert, onDismiss = viewModel::dismissAlert)
        } else {
            DashboardContent(
                uiState = uiState,
                onToggleSession = viewModel::toggleSession,
                onLogout = { viewModel.logout(onLoggedOut) },
                onRetryPermission = viewModel::requestPermissionAgain,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    onToggleSession: () -> Unit,
    onLogout: () -> Unit,
    onRetryPermission: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Anillo decorativo -- no es una meta real, solo referencia visual de que tan cerca esta
        // el pulso de un valor alto (200 bpm como techo arbitrario).
        CircularProgressIndicator(
            progress = ((uiState.bpm ?: 0) / 200f).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            if (uiState.permissionState == PermissionState.DENIED) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Sin permiso de pulso — toca para reintentar",
                    style = MaterialTheme.typography.caption3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onRetryPermission() },
                )
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "heartScale",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale },
                    )
                    Text(
                        text = uiState.bpm?.toString() ?: "--",
                        style = MaterialTheme.typography.display2,
                        color = if (uiState.bpmAccuracyLow) {
                            MaterialTheme.colors.onSurfaceVariant
                        } else {
                            MaterialTheme.colors.onSurface
                        },
                    )
                }
                Text(text = "BPM", style = MaterialTheme.typography.caption3)
            }

            Text(text = formatRelativeTime(uiState.lastReadingAt), style = MaterialTheme.typography.caption3)

            uiState.sendError?.let { error ->
                Text(text = error, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption3)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButtonCircle(
                    icon = if (uiState.sessionState == SessionState.INICIADA) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.sessionState == SessionState.INICIADA) "Pausar" else "Iniciar",
                    enabled = uiState.permissionState == PermissionState.GRANTED,
                    onClick = onToggleSession,
                )
                IconButtonCircle(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir",
                    onClick = onLogout,
                )
            }
        }
    }
}

@Composable
private fun AlertScreen(alert: ActiveAlert, onDismiss: () -> Unit) {
    val backgroundColor = when (alert.kind) {
        AlertKind.HIGH -> Color(0xFFB00020)
        AlertKind.LOW -> Color(0xFF1565C0)
    }
    val title = when (alert.kind) {
        AlertKind.HIGH -> "¡Pulso crítico!"
        AlertKind.LOW -> "¡Pulso bajo!"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Text(text = title, color = Color.White, style = MaterialTheme.typography.title3)
            Text(text = "${alert.bpm} BPM", color = Color.White, style = MaterialTheme.typography.display2)
            Text(text = "Toca para continuar", color = Color.White, style = MaterialTheme.typography.caption3)
        }
    }
}

@Composable
private fun IconButtonCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

private fun formatRelativeTime(instant: Instant?): String {
    if (instant == null) return "Sin datos"
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return "hace ${seconds}s"
}
