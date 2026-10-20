package com.example.milo_2.screens.StartScreen

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.example.milo_2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * MiloAudioPlayer
 * ===============
 * Handles the signature sonic branding sound for the MILO splash/intro animation.
 *
 * Architecture:
 * 1. Primary: Plays the studio-rendered `R.raw.milo_sonic_theme` using Android MediaPlayer.
 * 2. Fallback: If raw resource is missing or player encounters device codec errors,
 *    procedurally synthesizes the exact cinematic chord and sub-bass impact in real-time
 *    using native Android AudioTrack.
 */
class MiloAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the signature MILO intro sonic chord.
     * Non-blocking: Executes on IO dispatcher.
     */
    fun playIntroSound(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try playing from res/raw/milo_sonic_theme.wav
                val rawId = context.resources.getIdentifier("milo_sonic_theme", "raw", context.packageName)
                if (rawId != 0) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(context, rawId)?.apply {
                        setVolume(1.0f, 1.0f)
                        setOnCompletionListener {
                            it.release()
                            mediaPlayer = null
                        }
                        start()
                    }
                } else {
                    playSynthesizedFallback()
                }
            } catch (e: Exception) {
                Log.w("MiloAudioPlayer", "MediaPlayer failed, falling back to procedural audio", e)
                playSynthesizedFallback()
            }
        }
    }

    /**
     * Procedural synthesis fallback using AudioTrack.
     * Synthesizes:
     * - Rising frequency whoosh (representing characters rushing inward)
     * - Punchy 55Hz sub-bass impact (representing the fusion point)
     * - Major 9th shimmering chord (representing energy, friendship, and connection)
     */
    private fun playSynthesizedFallback() {
        try {
            val sampleRate = 44100
            val durationSeconds = 3.0
            val numSamples = (sampleRate * durationSeconds).toInt()
            val buffer = ShortArray(numSamples)
            val tImpact = 1.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var sample = 0.0

                // 1. Swarm Whoosh (0.2s to 1.0s)
                if (t in 0.2..tImpact) {
                    val progress = (t - 0.2) / (tImpact - 0.2)
                    val freq = 150.0 + 350.0 * Math.pow(progress, 2.0)
                    val amp = 0.3 * Math.pow(progress, 1.8)
                    sample += sin(2.0 * Math.PI * freq * t) * amp
                }

                // 2. Convergence Impact Chord (t >= 1.0s)
                if (t >= tImpact) {
                    val dt = t - tImpact
                    // Sub-bass punch (60Hz -> 40Hz)
                    val subEnv = exp(-dt * 4.2)
                    val subPitch = 60.0 * exp(-dt * 3.0) + 40.0
                    sample += sin(2.0 * Math.PI * subPitch * dt) * subEnv * 0.45

                    // Warm harmonic chord (A3, C#4, E4, G#4, B4)
                    val chordEnv = exp(-dt * 1.5)
                    val chordFreqs = doubleArrayOf(220.0, 277.18, 329.63, 415.30, 493.88)
                    for (f in chordFreqs) {
                        sample += sin(2.0 * Math.PI * f * dt) * chordEnv * 0.10
                    }

                    // Bright crystalline bell overtone
                    val chimeEnv = exp(-dt * 2.2)
                    sample += sin(2.0 * Math.PI * 1760.0 * dt) * chimeEnv * 0.06
                    sample += sin(2.0 * Math.PI * 2640.0 * dt) * chimeEnv * 0.04
                }

                // Clamping to 16-bit PCM
                val clamped = max(-1.0, min(1.0, sample * 0.85))
                buffer[i] = (clamped * 32767.0).toInt().toShort()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val track = AudioTrack(
                audioAttributes,
                audioFormat,
                buffer.size * 2,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(buffer, 0, buffer.size)
            track.play()

            // Release after playback
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay((durationSeconds * 1000).toLong() + 200)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("MiloAudioPlayer", "AudioTrack synthesis error", e)
        }
    }

    /**
     * Release player resources.
     */
    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }
}
