package com.auraclone.data

/**
 * Converts Pronto Hex format IR codes to frequency and pattern arrays.
 * 
 * Pronto Hex format example:
 * "0000 006D 0000 0022 00AC 00AC 0015 0015 0015 0015 0015 0040 0015 0015 0015 0015..."
 * 
 * Format: [Frequency code] [Repeat count] [Burst pairs...]
 * Each value is in hex and represents time units (0.241246 microseconds per unit)
 */
object ProntoHexConverter {
    
    private const val PRONTO_UNIT_MICROSECONDS = 0.241246
    
    /**
     * Converts Pronto Hex string to frequency (Hz) and pattern (microseconds)
     * @param prontoHex Space-separated hex values (e.g., "0000 006D 0000 0022 00AC...")
     * @return Pair of (frequency in Hz, pattern array in microseconds)
     */
    fun convert(prontoHex: String): Pair<Int, IntArray> {
        val parts = prontoHex.trim().split("\\s+".toRegex())
            .mapNotNull { it.toIntOrNull(16) }
        
        if (parts.size < 4) {
            throw IllegalArgumentException("Invalid Pronto Hex format: too few values")
        }
        
        // First value is frequency code (0 = use second value, otherwise it's a frequency code)
        val frequencyCode = parts[0]
        val frequency = if (frequencyCode == 0) {
            // Use second value as frequency (in kHz, convert to Hz)
            parts[1] * 1000
        } else {
            // Frequency code lookup (common values)
            when (frequencyCode) {
                0x0001 -> 30000  // 30 kHz
                0x0002 -> 33333  // 33.333 kHz
                0x0003 -> 36000  // 36 kHz
                0x0004 -> 38000  // 38 kHz (most common)
                0x0005 -> 40000  // 40 kHz
                0x0006 -> 56000  // 56 kHz
                else -> 38000   // Default to 38 kHz
            }
        }
        
        // Third value is repeat count (usually 0 for single transmission)
        // Fourth value is number of burst pairs
        val repeatCount = parts[2]
        val burstPairCount = parts[3]
        
        // Remaining values are burst pairs (mark, space, mark, space...)
        val pattern = mutableListOf<Int>()
        val burstPairs = parts.drop(4)
        
        for (i in burstPairs.indices step 2) {
            if (i + 1 < burstPairs.size) {
                val mark = (burstPairs[i] * PRONTO_UNIT_MICROSECONDS).toInt()
                val space = (burstPairs[i + 1] * PRONTO_UNIT_MICROSECONDS).toInt()
                pattern.add(mark)
                pattern.add(space)
            } else {
                // Odd number of values, last one is a mark
                val mark = (burstPairs[i] * PRONTO_UNIT_MICROSECONDS).toInt()
                pattern.add(mark)
            }
        }
        
        return Pair(frequency, pattern.toIntArray())
    }
    
    /**
     * Validates if a string looks like Pronto Hex format
     */
    fun isValidProntoHex(prontoHex: String): Boolean {
        val parts = prontoHex.trim().split("\\s+".toRegex())
        if (parts.size < 4) return false
        
        return parts.all { part ->
            part.matches("[0-9A-Fa-f]+".toRegex()) && part.length <= 4
        }
    }
}

