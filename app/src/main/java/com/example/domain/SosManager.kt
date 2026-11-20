package com.example.domain

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

class SosManager(
    private val context: Context,
    private val messageDao: MessageDao
) {
    val activeIncomingSos: Flow<List<MessageEntity>> = messageDao.observeActiveIncomingSos()
    val allSosHistory: Flow<List<MessageEntity>> = messageDao.observeAllSosMessages()

    fun triggerAlertHaptics(vibrateEnabled: Boolean) {
        if (!vibrateEnabled) return
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 280, 140, 280, 140, 420)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        }
    }

    suspend fun acknowledgeAlert(messageId: String) {
        messageDao.markMessageAcknowledged(messageId)
    }
}
