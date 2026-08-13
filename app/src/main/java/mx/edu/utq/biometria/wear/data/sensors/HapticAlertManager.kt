package mx.edu.utq.biometria.wear.data.sensors

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Patron de vibracion distinto al feedback normal de botones -- para que la alerta critica se
// sienta claramente diferente aunque el usuario no este mirando la pantalla en ese momento.
class HapticAlertManager(context: Context) {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    // USAGE_ALARM (no el default, que clasifica como TOUCH): el sistema bloquea vibraciones TOUCH
    // de apps que no estan en foreground estricto -- una alerta de pulso critico es exactamente
    // el tipo de evento que SI debe poder vibrar aunque el reloj este en modo ambiente/pantalla
    // atenuada, no un simple toque de boton.
    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .build()

    // Mismo patron para alerta alta o baja -- ambas son "necesita atencion ya", no hace falta
    // distinguirlas por tacto.
    fun vibrateAlert() {
        val pattern = longArrayOf(0, 300, 150, 300)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), alarmAttributes)
    }
}
