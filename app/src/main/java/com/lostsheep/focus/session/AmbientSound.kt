package com.lostsheep.focus.session

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soft, procedurally generated soundscape: wind, a quiet pad, the odd bird and a distant sheep.
 * Generated on the device (no audio files, no network) and faded in and out so it never startles.
 */
object AmbientSound {
    private const val RATE = 22050

    @Volatile private var generation = 0
    @Volatile private var playing = false

    @Synchronized
    fun startAmbient() {
        if (playing) return
        playing = true
        val myGen = ++generation
        Thread({ loop(myGen) }, "lost-sheep-ambient").apply { isDaemon = true }.start()
    }

    @Synchronized
    fun stopAmbient() {
        if (!playing) return
        playing = false
        generation++ // the running thread fades out and exits
    }

    private fun buildTrack(mode: Int, bytes: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(mode)
        .setBufferSizeInBytes(bytes)
        .build()

    private fun loop(myGen: Int) {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = runCatching { buildTrack(AudioTrack.MODE_STREAM, max(minBuf, RATE)) }.getOrNull() ?: return
        val synth = Landscape(Random(System.nanoTime()))
        val buf = ShortArray(1024)
        var fade = 0.0
        try {
            track.play()
            while (true) {
                val target = if (myGen == generation) 1.0 else 0.0
                for (i in buf.indices) {
                    fade += (target - fade) * 0.00004 // roughly a two-second fade
                    buf[i] = (synth.next() * fade * 32767).toInt().coerceIn(-32767, 32767).toShort()
                }
                track.write(buf, 0, buf.size)
                if (target == 0.0 && fade < 0.002) break
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** A gentle two-note bell for the end of a session. */
    fun playCompletionChime() {
        Thread(chime@{
            val seconds = 3.2
            val n = (RATE * seconds).toInt()
            val pcm = ShortArray(n)
            val notes = listOf(0.0 to 659.25, 0.42 to 987.77) // E5 then B5
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                var v = 0.0
                for ((start, f) in notes) {
                    val lt = t - start
                    if (lt < 0) continue
                    val env = (1 - exp(-lt * 60)) * exp(-lt * 2.2)
                    v += env * (sin(2 * PI * f * lt) + 0.28 * sin(2 * PI * f * 2.76 * lt) + 0.08 * sin(2 * PI * f * 5.4 * lt))
                }
                pcm[i] = (v * 0.16 * 32767).toInt().coerceIn(-32767, 32767).toShort()
            }
            val track = runCatching { buildTrack(AudioTrack.MODE_STATIC, n * 2) }.getOrNull() ?: return@chime
            try {
                track.write(pcm, 0, n)
                track.play()
                Thread.sleep((seconds * 1000).toLong() + 200)
            } catch (_: Exception) {
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }, "lost-sheep-chime").apply { isDaemon = true }.start()
    }

    /** One sample at a time, mixed very quietly. */
    private class Landscape(private val rnd: Random) {
        private var t = 0.0
        private val dt = 1.0 / RATE

        // Wind: twice-filtered noise with a slow swell.
        private var w1 = 0.0
        private var w2 = 0.0

        // Birds.
        private var nextBird = 4.0 + rnd.nextDouble() * 6
        private var birdStart = -10.0
        private var birdPitch = 3200.0
        private var birdPhase = 0.0

        // Distant sheep.
        private var nextSheep = 25.0 + rnd.nextDouble() * 30
        private var sheepStart = -10.0
        private var sheepPhase = 0.0
        private var sheepLp = 0.0

        fun next(): Double {
            t += dt

            val noise = rnd.nextDouble() * 2 - 1
            w1 += (noise - w1) * 0.02
            w2 += (w1 - w2) * 0.05
            val swell = 0.55 + 0.45 * sin(2 * PI * 0.045 * t) * sin(2 * PI * 0.013 * t + 1.3)
            val wind = w2 * 2.6 * swell

            val padEnv = 0.65 + 0.35 * sin(2 * PI * t / 22.0)
            val pad = padEnv * (sin(2 * PI * 196.0 * t) + 0.8 * sin(2 * PI * 246.94 * t) + 0.7 * sin(2 * PI * 293.66 * t)) / 2.5

            if (t >= nextBird) {
                birdStart = t
                birdPitch = 2800.0 + rnd.nextDouble() * 1200
                nextBird = t + 7 + rnd.nextDouble() * 14
            }
            var bird = 0.0
            val bt = t - birdStart
            if (bt in 0.0..0.5) {
                val note = (bt / 0.14).toInt()
                val nt = bt - note * 0.14
                if (note < 3 && nt < 0.08) {
                    val f = birdPitch + 1400 * (nt / 0.08)
                    birdPhase += 2 * PI * f * dt
                    bird = sin(PI * nt / 0.08) * sin(birdPhase)
                }
            }

            if (t >= nextSheep) {
                sheepStart = t
                nextSheep = t + 45 + rnd.nextDouble() * 70
            }
            var sheep = 0.0
            val st = t - sheepStart
            if (st in 0.0..0.75) {
                val f = 205 + 9 * sin(2 * PI * 7 * st)
                sheepPhase += 2 * PI * f * dt
                var v = 0.0
                for (h in 1..6) v += sin(sheepPhase * h) / (h * h * 0.6 + 0.4)
                val env = sin(PI * st / 0.75) * (0.75 + 0.25 * sin(2 * PI * 9 * st))
                sheepLp += (v * env - sheepLp) * 0.15 // soften: it is far away
                sheep = sheepLp
            }

            return wind * 0.20 + pad * 0.035 + bird * 0.045 + sheep * 0.03
        }
    }
}
