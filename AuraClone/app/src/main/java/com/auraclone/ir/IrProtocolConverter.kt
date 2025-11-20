package com.auraclone.ir

/**
 * Converts protocol-based IR codes (e.g., "NEC,32,159,0") to frequency and pattern arrays.
 */
object IrProtocolConverter {

    /**
     * Converts a protocol string to frequency (Hz) and pattern (microseconds).
     * Supported protocols: NEC
     * 
     * @param protocolString Format: "PROTOCOL,ARG1,ARG2,..." (e.g., "NEC,32,159,0")
     * @return Pair of (frequency in Hz, pattern array in microseconds) or null if not supported
     */
    fun convert(protocolString: String): Pair<Int, IntArray>? {
        val parts = protocolString.split(",")
        if (parts.isEmpty()) return null

        val protocol = parts[0].uppercase()
        
        return when (protocol) {
            "NEC" -> convertNEC(parts)
            else -> null
        }
    }

    /**
     * Converts NEC protocol arguments to pattern.
     * NEC Format: Address (8-bit), Command (8-bit)
     * Standard NEC: Address, ~Address, Command, ~Command
     * Extended NEC: Address Low, Address High, Command, ~Command
     * 
     * Arguments expected: "NEC,address,command,extra"
     */
    private fun convertNEC(parts: List<String>): Pair<Int, IntArray>? {
        if (parts.size < 3) return null

        try {
            val address = parts[1].toIntOrNull() ?: return null
            val command = parts[2].toIntOrNull() ?: return null
            
            // NEC uses 38kHz carrier
            val frequency = 38000
            
            val pattern = mutableListOf<Int>()
            
            // Leader code: 9ms mark, 4.5ms space
            pattern.add(9000)
            pattern.add(4500)
            
            // Address (8 bits)
            // Note: NEC sends LSB first
            addByte(pattern, address)
            
            // Logical inverse of Address (for standard NEC)
            // Or Address High (for extended). 
            // The data format "NEC,32,159,0" is ambiguous about extended vs standard.
            // We'll assume standard NEC (Address, ~Address) for now.
            addByte(pattern, address.inv())
            
            // Command (8 bits)
            addByte(pattern, command)
            
            // Logical inverse of Command
            addByte(pattern, command.inv())
            
            // End bit: 560us mark
            pattern.add(560)
            
            return Pair(frequency, pattern.toIntArray())
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun addByte(pattern: MutableList<Int>, byteVal: Int) {
        var value = byteVal
        for (i in 0 until 8) {
            val bit = value and 1
            // Bit mark is always 560us
            pattern.add(560)
            
            if (bit == 1) {
                // Logical 1: 1690us space
                pattern.add(1690)
            } else {
                // Logical 0: 560us space
                pattern.add(560)
            }
            value = value shr 1
        }
    }
}
