package com.fivegguardian

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log

class AlarmPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    fun start() {
        if (mediaPlayer?.isPlaying == true || toneGenerator != null) {
            Log.d("AlarmPlayer", "Alarm is already playing, skipping start.")
            return
        }

        val toneId = NetworkMonitorManager.selectedAlarmToneId.value
        val toneList = NetworkMonitorManager.alarmTonesList.value
        val soundOption = toneList.find { it.id == toneId } ?: SoundOption("sys_alarm", "Default System Alarm")

        Log.d("AlarmPlayer", "Starting alarm sound: ${soundOption.title}")

        if (soundOption.isTone) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100).apply {
                    startTone(soundOption.toneType, 120000)
                }
                NetworkMonitorManager.updateAlarmPlaying(true)
                Log.d("AlarmPlayer", "ToneGenerator alarm started successfully: ${soundOption.title}")
            } catch (e: Exception) {
                Log.e("AlarmPlayer", "Failed to start ToneGenerator alarm, trying fallback", e)
                startToneFallback()
            }
        } else {
            try {
                val alarmUri = soundOption.uriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri == null) {
                    startToneFallback()
                    return
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
                NetworkMonitorManager.updateAlarmPlaying(true)
                Log.d("AlarmPlayer", "MediaPlayer alarm started playing: ${soundOption.title}")
            } catch (e: Exception) {
                Log.e("AlarmPlayer", "Failed to start alarm player with preferred URI, trying fallback", e)
                startToneFallback()
            }
        }
    }

    private fun startToneFallback() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100).apply {
                startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 120000)
            }
            NetworkMonitorManager.updateAlarmPlaying(true)
            Log.d("AlarmPlayer", "ToneGenerator fallback started successfully.")
        } catch (toneEx: Exception) {
            Log.e("AlarmPlayer", "ToneGenerator fallback also failed", toneEx)
        }
    }

    @Synchronized
    fun playOneShotNotification() {
        val toneId = NetworkMonitorManager.selectedNotificationToneId.value
        val toneList = NetworkMonitorManager.notificationTonesList.value
        val soundOption = toneList.find { it.id == toneId } ?: SoundOption("sys_notification", "Default System Notification")

        Log.d("AlarmPlayer", "Playing one-shot notification: ${soundOption.title}")

        try {
            if (soundOption.isTone) {
                val localToneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                localToneGen.startTone(soundOption.toneType, 300)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        localToneGen.release()
                    } catch (e: Exception) {}
                }, 1000)
            } else {
                val uri = soundOption.uriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (uri != null) {
                    val oneShotPlayer = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        prepare()
                        start()
                    }
                    oneShotPlayer.setOnCompletionListener {
                        try {
                            it.release()
                        } catch (e: Exception) {}
                    }
                } else {
                    val fallbackGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                    fallbackGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try { fallbackGen.release() } catch (e: Exception) {}
                    }, 1000)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmPlayer", "Failed to play one-shot notification", e)
            try {
                val fallbackGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                fallbackGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { fallbackGen.release() } catch (e: Exception) {}
                }, 1000)
            } catch (ex: Exception) {}
        }
    }

    @Synchronized
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmPlayer", "Error stopping alarm player", e)
        } finally {
            mediaPlayer = null
        }

        try {
            toneGenerator?.let {
                it.stopTone()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmPlayer", "Error stopping tone generator", e)
        } finally {
            toneGenerator = null
            NetworkMonitorManager.updateAlarmPlaying(false)
            Log.d("AlarmPlayer", "Alarm/Tone stopped playing.")
        }
    }
}
