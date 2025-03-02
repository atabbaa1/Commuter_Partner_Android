package com.example.commuterpartner

import android.content.Context
import android.media.SoundPool
import android.media.AudioAttributes
import android.util.Log

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundId: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundId = soundPool.load(context, R.raw.ringtone, 1)
    }

    fun playSound() {
        val result = soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        Log.d("SoundTest", "Sound played with result: $result") // TODO: Remove this line in the future
    }
}