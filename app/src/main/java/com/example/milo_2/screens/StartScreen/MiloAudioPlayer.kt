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
            val durationSeconds = 4.8
            val numSamples = (sampleRate * durationSeconds).toInt()
            val buffer = ShortArray(numSamples)
            val tImpact = 2.4

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var sample = 0.0

                // 0. Ambient intro pad (0.0s to 1.0s)
                if (t < 1.0) {
                    val padEnv = 0.5 * (1.0 - kotlin.math.cos(Math.PI * t / 1.0)) * 0.08
                    val padOsc = sin(2.0 * Math.PI * 110.0 * t) + 0.4 * sin(2.0 * Math.PI * 220.0 * t)
                    sample += padOsc * padEnv
                }

                // 1. Smooth Rising Swarm / Whoosh (0.4s to 2.4s)
                if (t in 0.4..tImpact) {
                    val progress = (t - 0.4) / (tImpact - 0.4)
                    val smoothP = 0.5 * (1.0 - kotlin.math.cos(Math.PI * progress))
                    val freq = 130.0 + 380.0 * Math.pow(smoothP, 1.8)
                    val amp = 0.26 * Math.pow(smoothP, 1.5)
                    sample += sin(2.0 * Math.PI * freq * t) * amp
                }

                // 2. Convergence Impact Chord (t >= 2.4s)
                if (t >= tImpact) {
                    val dt = t - tImpact
                    // Sub-bass punch (62Hz -> 38Hz)
                    val subEnv = exp(-dt * 3.2)
                    val subPitch = 62.0 * exp(-dt * 2.5) + 38.0
                    sample += sin(2.0 * Math.PI * subPitch * dt) * subEnv * 0.48

                    // Warm harmonic Major 9th chord (A2, A3, C#4, E4, G#4, B4)
                    val chordEnv = exp(-dt * 1.1)
                    val chordFreqs = doubleArrayOf(110.0, 220.0, 277.18, 329.63, 415.30, 493.88)
                    val weights = doubleArrayOf(0.08, 0.10, 0.09, 0.08, 0.07, 0.06)
                    for (idx in chordFreqs.indices) {
                        val f = chordFreqs[idx]
                        val w = weights[idx]
                        val osc = sin(2.0 * Math.PI * f * dt) + 0.28 * sin(2.0 * Math.PI * (f * 2.005) * dt)
                        sample += osc * chordEnv * w
                    }

                    // Bright crystalline bell overtones
                    val chimeEnv = exp(-dt * 1.8)
                    val chimes = doubleArrayOf(880.0, 1318.5, 1760.0, 2637.0)
                    for (cf in chimes) {
                        sample += sin(2.0 * Math.PI * cf * dt) * chimeEnv * 0.035
                    }
                }

                // Clamping to 16-bit PCM
                val clamped = max(-1.0, min(1.0, sample * 0.88))
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
