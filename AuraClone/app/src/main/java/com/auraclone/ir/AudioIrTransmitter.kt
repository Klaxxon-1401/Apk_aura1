package com.auraclone.ir

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class AudioIrTransmitter : IrTransmitter {
    private val sampleRate = 44100

    override fun hasIrEmitter(): Boolean {
        // Assume audio jack is always available for potential IR blaster
        return true
    }

    override fun transmit(frequency: Int, pattern: IntArray) {
        val buffer = generateAudioBuffer(frequency, pattern)
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO, // Stereo for better compatibility with some blasters
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STATIC
        )

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        // Release after playback (in a real app, handle this better to avoid blocking/leaks)
        // For now, we let it play out.
    }

    private fun generateAudioBuffer(frequency: Int, pattern: IntArray): ShortArray {
        val durationUs = pattern.sum()
        val numSamples = (durationUs * sampleRate / 1000000).toInt()
        val buffer = ShortArray(numSamples * 2) // Stereo

        var sampleIndex = 0
        var isMark = true // Start with Mark (Pulse)

        // Carrier wave period in samples
        val carrierPeriod = sampleRate.toDouble() / frequency
        var phase = 0.0

        for (segmentUs in pattern) {
            val segmentSamples = (segmentUs * sampleRate / 1000000).toInt()
            
            for (i in 0 until segmentSamples) {
                if (sampleIndex >= numSamples) break

                val value: Short
                if (isMark) {
                    // Generate sine wave for carrier
                    val angle = 2.0 * Math.PI * phase
                    value = (Math.sin(angle) * Short.MAX_VALUE).toInt().toShort()
                    phase += 1.0 / carrierPeriod
                    if (phase > 1.0) phase -= 1.0
                } else {
                    value = 0
                    phase = 0.0 // Reset phase for next mark? Or keep continuous? Usually reset is fine for IR.
                }

                // Write to both channels (Stereo) - some blasters use L/R phase difference, but simple ones use both.
                // Inverting one channel is a common trick for voltage doubling on some passive blasters.
                // Let's try standard in-phase first.
                buffer[sampleIndex * 2] = value
                buffer[sampleIndex * 2 + 1] = (-value).toShort() // Inverted right channel for voltage doubling

                sampleIndex++
            }
            isMark = !isMark
        }
        return buffer
    }

    override fun getName(): String = "Audio Jack IR Blaster"
}
