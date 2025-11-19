package com.auraclone.ir

interface IrTransmitter {
    fun hasIrEmitter(): Boolean
    fun transmit(frequency: Int, pattern: IntArray)
    fun getName(): String
}
