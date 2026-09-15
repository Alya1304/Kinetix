package com.yach.kinetix.backofhouse.domain

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KitchenAlertManager @Inject constructor() {

    /**
     * Emite un tono o alerta sonora audible en el dispositivo KDS cuando ingresa una nueva comanda a cocina.
     */
    suspend fun playNewOrderSound() = withContext(Dispatchers.Default) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        } catch (_: Exception) {
            // Manejo de contingencia silenciosa en dispositivos sin hardware de audio
        }
    }

    /**
     * Emite un aviso sonoro diferenciado para alertar que un platillo u orden está lista para entregar.
     */
    suspend fun playOrderReadySound() = withContext(Dispatchers.Default) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (_: Exception) {}
    }
}
