package com.tplatt.shadowshaper

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Deterministic 22.05 kHz mono PCM source. Builds a small set of WAV samples
 * ONCE off the UI thread, for Android SoundPool to play with low latency.
 *
 * No generated tone drives gameplay timing. Samples are intentional original
 * sci-fi sounds, not a claim to reproduce protected Destiny game recordings.
 */
internal object DuelSoundSynthesis {
    const val RATE = 22050
    private const val TWO_PI = 2.0 * PI

    fun wav(sound: DuelSound): ByteArray {
        val samples = (sound.durationMs * RATE / 1000).coerceAtLeast(1)
        val dataSize = samples * 2
        val output = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        output.put("RIFF".toByteArray(Charsets.US_ASCII))
        output.putInt(36 + dataSize)
        output.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        output.putInt(16)
        output.putShort(1.toShort()) // PCM
        output.putShort(1.toShort()) // mono
        output.putInt(RATE)
        output.putInt(RATE * 2)
        output.putShort(2.toShort())
        output.putShort(16.toShort())
        output.put("data".toByteArray(Charsets.US_ASCII))
        output.putInt(dataSize)

        val random = Random(0x51B0 + sound.ordinal * 417)
        var phase = 0.0
        var phaseB = 0.0
        var filteredNoise = 0.0
        var prior = 0.0
        for (i in 0 until samples) {
            val t = i.toDouble() / RATE
            val p = i.toDouble() / max(1, samples - 1)
            val fadeIn = min(1.0, t / .018)
            val fadeOut = min(1.0, (1.0 - p) / .085)
            val baseEnv = (fadeIn * fadeOut).coerceIn(0.0, 1.0)
            val whiteNoise = random.nextDouble(-1.0, 1.0)
            filteredNoise = filteredNoise * .84 + whiteNoise * .16

            var signal = 0.0
            var envelope = baseEnv
            var freq = 440.0
            var freqB = 660.0
            when (sound) {
                DuelSound.UI_TAP -> {
                    freq = 1250.0 - 640.0 * p
                    envelope *= exp(-12.0 * p)
                    signal = .65 * filteredNoise + .35 * whiteNoise
                }
                DuelSound.CARD_SELECT -> {
                    freq = 930.0 + 350.0 * (1.0 - p)
                    freqB = freq * 1.50
                    envelope *= exp(-4.7 * p)
                    signal = .76 * sin(phase) + .20 * sin(phaseB) +
                        .12 * whiteNoise * (1.0 - p)
                }
                DuelSound.TRANSMAT_DEFAULT -> {
                    freq = 265.0 + 505.0 * p
                    freqB = freq * 1.51
                    envelope *= (.57 + .43 * sin(PI * p)) *
                        (1.0 - .19 * p)
                    signal = .52 * sin(phase) + .26 * sin(phaseB) +
                        .30 * filteredNoise + .14 * sin(phase * 3.01)
                }
                DuelSound.TRANSMAT_HIVE -> {
                    freq = 103.0 + 56.0 * sin(PI * p * 1.4)
                    freqB = 199.0 - 26.0 * p
                    val ritualPulse = .60 + .40 * abs(sin(TWO_PI * t * 13.0))
                    envelope *= ritualPulse * (.8 - .19 * p)
                    signal = .60 * sin(phase) + .26 * sin(phase * 2.08) +
                        .25 * sin(phaseB) + .37 * filteredNoise
                }
                DuelSound.TRANSMAT_CABAL -> {
                    freq = 160.0 - 87.0 * p
                    freqB = 320.0 - 150.0 * p
                    envelope *= (.70 + .30 * exp(-9 * p))
                    val stomp = exp(-16.0 * p)
                    signal = .69 * sin(phase) + .20 * sin(phaseB) +
                        .44 * filteredNoise * (.5 + stomp) +
                        .25 * whiteNoise * stomp
                }
                DuelSound.TRANSMAT_VEX -> {
                    freq = 386.0 + (if ((p * 8).toInt() % 2 == 0) 208 else 412)
                    freqB = 813.0 + 131.0 * sin(PI * p * 5.0)
                    val digitalGate = if ((t * 42).toInt() % 7 == 0) .35 else 1.0
                    envelope *= .8 * digitalGate
                    signal = .45 * sin(phase) + .33 * sin(phaseB) +
                        .18 * sin(phaseB * 2.01) + .20 * filteredNoise
                }
                DuelSound.TRANSMAT_FALLEN -> {
                    freq = 370.0 + 780.0 * abs(sin(TWO_PI * t * 7.0))
                    freqB = freq * 1.86
                    val sparkGate = if ((t * 77).toInt() % 5 < 2) 1.0 else .37
                    envelope *= sparkGate
                    signal = .36 * sin(phase) + .31 * sin(phaseB) +
                        .43 * whiteNoise * sparkGate + .26 * filteredNoise
                }
                DuelSound.CARD_LAND -> {
                    freq = 105.0 * exp(-2.1 * p) + 33.0
                    freqB = 225.0 * exp(-3.2 * p) + 80.0
                    envelope *= exp(-8.0 * p)
                    signal = .90 * sin(phase) + .27 * sin(phaseB) +
                        .40 * whiteNoise * exp(-14.0 * p)
                }
                DuelSound.DAMAGE -> {
                    freq = 240.0 - 165.0 * p
                    freqB = 480.0 - 220.0 * p
                    envelope *= exp(-6.0 * p)
                    signal = .36 * sin(phase) + .28 * sin(phaseB) +
                        .57 * whiteNoise + .31 * filteredNoise
                }
                DuelSound.DESTROY -> {
                    freq = 160.0 - 102.0 * p
                    freqB = 340.0 - 160.0 * p
                    envelope *= exp(-4.2 * p)
                    signal = .47 * sin(phase) + .32 * sin(phaseB) +
                        .48 * whiteNoise + .31 * filteredNoise
                }
                DuelSound.BUFF -> {
                    freq = 442.0 + 510.0 * p
                    freqB = freq * 1.498
                    envelope *= (1.0 - .25 * p)
                    signal = .48 * sin(phase) + .33 * sin(phaseB) +
                        .22 * sin(phase * 2.0) + .11 * filteredNoise
                }
                DuelSound.SHIELD -> {
                    freq = 725.0 - 250.0 * p
                    freqB = freq * 1.492
                    envelope *= exp(-1.4 * p)
                    signal = .53 * sin(phase) + .34 * sin(phaseB) +
                        .19 * sin(phaseB * 1.89) + .10 * filteredNoise
                }
                DuelSound.STATUS -> {
                    freq = 280.0 + 160.0 * sin(PI * p * 3.0)
                    freqB = 715.0 - 240.0 * p
                    envelope *= exp(-2.0 * p)
                    signal = .46 * sin(phase) + .20 * sin(phaseB) +
                        .36 * filteredNoise
                }
                DuelSound.TURN -> {
                    freq = if (p < .45) 330.0 else 440.0
                    freqB = freq * 1.498
                    envelope *= .82
                    signal = .58 * sin(phase) + .27 * sin(phaseB) +
                        .13 * sin(phaseB * 2.0)
                }
                DuelSound.WIN -> {
                    freq = if (p < .25) 330.0 else if (p < .53) 440.0 else 550.0
                    freqB = freq * 1.50
                    envelope *= .86
                    signal = .46 * sin(phase) + .25 * sin(phaseB) +
                        .22 * sin(phase * 2.01) + .08 * filteredNoise
                }
                DuelSound.LOSE -> {
                    freq = 225.0 - 72.0 * p
                    freqB = freq * 1.32
                    envelope *= exp(-.95 * p)
                    signal = .48 * sin(phase) + .32 * sin(phaseB) +
                        .13 * filteredNoise
                }
                DuelSound.DRAW_SINGLE -> {
                    // A bright paper-edge scrape with a soft sci-fi swish.
                    freq = 610.0 + 470.0 * (1.0 - p)
                    freqB = 1680.0 - 470.0 * p
                    val edge = exp(-8.5 * p)
                    envelope *= (.40 + .60 * edge)
                    signal = .60 * whiteNoise * edge +
                        .55 * filteredNoise + .20 * sin(phase) +
                        .10 * sin(phaseB)
                }
                DuelSound.DRAW_MULTI -> {
                    // Several quick swipes inside one event, not a sustained
                    // noisy shuffling loop.
                    freq = 560.0 + 220.0 * p
                    freqB = 1220.0 + 110.0 * p
                    val staggered = (1.0 + cos(TWO_PI * p * 3.0)) * .5
                    envelope *= .34 + .66 * staggered
                    signal = .56 * filteredNoise +
                        .38 * whiteNoise * staggered +
                        .18 * sin(phase) + .14 * sin(phaseB)
                }
                DuelSound.SHUFFLE -> {
                    // Fine-grain two-handed riffle with periodic card edges.
                    freq = 295.0 + 135.0 * sin(TWO_PI * p * 3.5)
                    freqB = 940.0 - 250.0 * p
                    val flutter = .35 + .65 * abs(sin(TWO_PI * t * 24.0))
                    envelope *= flutter * (.86 - .20 * p)
                    signal = .61 * whiteNoise * flutter +
                        .49 * filteredNoise +
                        .11 * sin(phase) + .06 * sin(phaseB)
                }
                DuelSound.OPENING_DEAL -> {
                    // A tidy set of clipped swishes and subtle UI chimes.
                    freq = 680.0 + 270.0 * p
                    freqB = 1140.0 + 180.0 * p
                    val deal = exp(-9.0 * ((p * 4.0) % 1.0))
                    envelope *= .42 + .58 * deal
                    signal = .37 * whiteNoise * deal +
                        .44 * filteredNoise + .22 * sin(phase) +
                        .13 * sin(phaseB)
                }
                DuelSound.MULLIGAN -> {
                    // Reserved for a future real redraw action.
                    freq = 680.0 - 400.0 * p
                    freqB = 1040.0 - 270.0 * p
                    envelope *= .56 + .32 * abs(sin(TWO_PI * p * 5.0))
                    signal = .48 * whiteNoise + .38 * filteredNoise +
                        .22 * sin(phase) + .13 * sin(phaseB)
                }
                DuelSound.DISCARD -> {
                    // One sharper flick, distinct from an incoming draw.
                    freq = 810.0 - 610.0 * p
                    freqB = 1580.0 - 900.0 * p
                    envelope *= exp(-7.0 * p)
                    signal = .69 * whiteNoise + .43 * filteredNoise +
                        .13 * sin(phase) + .08 * sin(phaseB)
                }
                DuelSound.CARD_LIFT -> {
                    freq = 790.0 + 470.0 * p
                    freqB = 1710.0 - 410.0 * p
                    envelope *= exp(-5.0 * p)
                    signal = .52 * filteredNoise + .29 * whiteNoise +
                        .22 * sin(phase) + .10 * sin(phaseB)
                }
                DuelSound.TARGET_LOCK -> {
                    // Brief high-precision holographic reticle confirmation.
                    freq = 560.0 + 220.0 * (if (p < .50) 0.0 else 1.0)
                    freqB = freq * 1.51
                    envelope *= exp(-1.5 * p)
                    signal = .54 * sin(phase) + .27 * sin(phaseB) +
                        .12 * sin(phaseB * 2.0) + .06 * filteredNoise
                }
                DuelSound.ABILITY_TRIGGER -> {
                    freq = 400.0 + 620.0 * p
                    freqB = freq * 1.49
                    envelope *= (.43 + .57 * sin(PI * p))
                    signal = .43 * sin(phase) + .29 * sin(phaseB) +
                        .18 * filteredNoise + .12 * sin(phase * 2.13)
                }
                DuelSound.CARD_BREAK -> {
                    // Short crystalline crack followed by dissolving static.
                    freq = 490.0 * exp(-3.0 * p) + 125.0
                    freqB = 1180.0 * exp(-2.5 * p)
                    envelope *= exp(-5.3 * p)
                    signal = .63 * whiteNoise + .48 * filteredNoise +
                        .27 * sin(phase) + .22 * sin(phaseB)
                }
                DuelSound.DECK_EMPTY -> {
                    freq = if (p < .46) 510.0 else 385.0
                    freqB = freq * 1.21
                    envelope *= exp(-2.8 * p)
                    signal = .56 * sin(phase) + .26 * sin(phaseB) +
                        .08 * filteredNoise
                }
            }
            phase += TWO_PI * freq / RATE
            phaseB += TWO_PI * freqB / RATE
            val mixed = tanh(signal * envelope * 1.12)
            // Avoid a DC offset on very low bass synth passes.
            val highPass = mixed - prior * .035
            prior = mixed
            output.putShort((highPass.coerceIn(-.95, .95) * 24000).toInt().toShort())
        }
        return output.array()
    }
}
