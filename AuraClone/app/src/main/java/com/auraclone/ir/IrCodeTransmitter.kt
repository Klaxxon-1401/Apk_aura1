package com.auraclone.ir

import com.auraclone.data.IrCode
import com.auraclone.data.IrRepository

/**
 * Helper class to transmit IR codes from IrCode objects
 */
class IrCodeTransmitter(
    private val irManager: IrManager,
    private val irRepository: IrRepository
) {
    /**
     * Transmit an IR code by brand, device, and function name
     */
    fun transmit(brand: String, device: String, function: String) {
        val code = irRepository.getCode(brand, device, function)
        if (code != null) {
            val (frequency, pattern) = code.getFrequencyAndPattern()
            irManager.transmit(frequency, pattern)
        } else {
            throw IllegalArgumentException("IR code not found: $brand/$device/$function")
        }
    }
    
    /**
     * Transmit an IR code directly from an IrCode object
     */
    fun transmit(code: IrCode) {
        val (frequency, pattern) = code.getFrequencyAndPattern()
        irManager.transmit(frequency, pattern)
    }
}

