package com.auraclone.ir

import android.content.Context

class IrManager(context: Context) {
    private val transmitters = listOf(
        BuiltInIrTransmitter(context),
        UsbIrTransmitter(context),
        AudioIrTransmitter()
    )

    fun getActiveTransmitter(): IrTransmitter {
        // Priority: Built-in > USB > Audio
        // Note: Audio is always "available" effectively, so it's the fallback.
        // We might want to check for actual USB device connection for USB.
        
        for (transmitter in transmitters) {
            if (transmitter.hasIrEmitter()) {
                return transmitter
            }
        }
        return transmitters.last() // Fallback to Audio
    }

    fun transmit(frequency: Int, pattern: IntArray) {
        getActiveTransmitter().transmit(frequency, pattern)
    }
}
