package com.auraclone.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class BuiltInIrTransmitter(context: Context) : IrTransmitter {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    override fun hasIrEmitter(): Boolean {
        return manager?.hasIrEmitter() == true
    }

    override fun transmit(frequency: Int, pattern: IntArray) {
        if (hasIrEmitter()) {
            manager?.transmit(frequency, pattern)
        }
    }

    override fun getName(): String = "Built-in IR Blaster"
}
