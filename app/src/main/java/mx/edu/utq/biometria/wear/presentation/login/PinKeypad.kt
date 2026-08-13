package mx.edu.utq.biometria.wear.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 6

// Teclado numerico armado a mano con Button/Row/Column de Wear Compose Material (no
// LazyVerticalGrid) para no depender de un artifact de Compose Foundation general no confirmado
// en este scaffold. 4 filas x 3 columnas: digitos 1-9, luego borrar / 0 / confirmar.
@Composable
fun PinKeypad(
    pinLength: Int,
    confirmEnabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 1..3) {
                    val digit = ('1' + (row * 3 + col - 1))
                    KeypadButton(label = digit.toString(), onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            KeypadButton(label = "⌫", onClick = onBackspace, enabled = pinLength > 0)
            KeypadButton(label = "0", onClick = { onDigit('0') })
            KeypadButton(
                label = "✓",
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.primaryButtonColors(),
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: androidx.wear.compose.material.ButtonColors = ButtonDefaults.secondaryButtonColors(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = Modifier.size(30.dp),
    ) {
        Text(text = label)
    }
}

// El PIN real puede tener 4, 5 o 6 digitos (confirmEnabled ya se habilita a partir de 4) -- el
// indicador muestra el minimo (4) por defecto en vez de siempre 6, para no dar a entender que
// hacen falta 6 digitos cuando en realidad alcanza con 4. Si el usuario elige un PIN mas largo,
// el indicador crece en vivo para acompanarlo.
fun buildPinIndicator(pin: String): String {
    val visibleLength = maxOf(MIN_PIN_LENGTH, pin.length)
    val filled = "●".repeat(pin.length)
    val empty = "○".repeat((visibleLength - pin.length).coerceAtLeast(0))
    return filled + empty
}

const val PinMaxLength = MAX_PIN_LENGTH
