package mx.edu.utq.biometria.wear.data.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import mx.edu.utq.biometria.wear.R
import mx.edu.utq.biometria.wear.presentation.MainActivity
import mx.edu.utq.biometria.wear.presentation.home.AlertKind

private const val CHANNEL_ID = "critical_heart_rate_alerts"
private const val NOTIFICATION_ID = 1001

// Notificacion push del sistema (heads-up) para la alerta de pulso -- complementa, no reemplaza,
// la pantalla roja/azul a pantalla completa dentro de la app y la vibracion de HapticAlertManager.
// Asi el usuario se entera aunque no este mirando la app en ese momento.
class AlertNotificationManager(private val context: Context) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de pulso",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos cuando el pulso sale del rango normal"
            // El patron de vibracion ya lo maneja HapticAlertManager (USAGE_ALARM) -- si el canal
            // tambien vibrara con su patron default, se duplicaria el buzz en cada alerta.
            enableVibration(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun postAlert(bpm: Int, kind: AlertKind) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val (title, text) = when (kind) {
            AlertKind.HIGH -> "¡Pulso crítico!" to "$bpm BPM — necesita atención inmediata"
            AlertKind.LOW -> "¡Pulso bajo!" to "$bpm BPM — posible bradicardia"
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ID fijo (no uno distinto por kind): una alerta nueva siempre reemplaza a la anterior en
        // vez de apilar notificaciones -- no puede haber pulso alto y bajo a la vez de todos modos.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
