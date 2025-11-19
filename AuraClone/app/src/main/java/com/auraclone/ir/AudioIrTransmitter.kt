package com.auraclone.ir

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Irdroid-style passive audio IR blaster transmitter
 * Modulates IR signal as audio that can be played through audio jack
 * Compatible with passive IR blasters that convert audio to IR signals
 */
class AudioIrTransmitter : IrTransmitter {
    private val sampleRate = 44100
    private var currentTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun hasIrEmitter(): Boolean {
        // Assume audio jack is always available for potential IR blaster
        return true
    }

    override fun transmit(frequency: Int, pattern: IntArray) {
        // Stop any currently playing transmission
        currentTrack?.stop()
        currentTrack?.release()
        
        scope.launch {
            try {
                val buffer = generateAudioBuffer(frequency, pattern)
                val audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            android.media.AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build()
                        )
                        .setBufferSizeInBytes(buffer.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    // Fallback for older Android versions
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        buffer.size * 2,
                        AudioTrack.MODE_STATIC
                    )
                }

                currentTrack = audioTrack
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                // Wait for playback to complete, then release
                val durationMs = (buffer.size * 1000) / (sampleRate * 2)
                delay(durationMs.toLong())
                
                audioTrack.stop()
                audioTrack.release()
                if (currentTrack == audioTrack) {
                    currentTrack = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
