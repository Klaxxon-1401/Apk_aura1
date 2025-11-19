package com.auraclone.data

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Pronto Hex to Frequency/Pattern conversion
 */
class ProntoHexConverterTest {

    @Test
    fun testValidProntoHex() {
        val prontoHex = "0000 006D 0000 0022 00AC 00AC 0015 0015"
        assertTrue(ProntoHexConverter.isValidProntoHex(prontoHex))
    }

    @Test
    fun testInvalidProntoHex() {
        assertFalse(ProntoHexConverter.isValidProntoHex("invalid"))
        assertFalse(ProntoHexConverter.isValidProntoHex("0000"))
        assertFalse(ProntoHexConverter.isValidProntoHex(""))
    }

    @Test
    fun testConvertProntoHexWithFrequencyCode() {
        // Example: 38kHz frequency code (0x0004)
        val prontoHex = "0004 0000 0000 0002 00AC 00AC"
        val (frequency, pattern) = ProntoHexConverter.convert(prontoHex)
        
        assertEquals(38000, frequency)
        assertTrue(pattern.isNotEmpty())
    }

    @Test
    fun testConvertProntoHexWithDirectFrequency() {
        // Example: Direct frequency in second value (38kHz = 0x0026 = 38 decimal)
        val prontoHex = "0000 0026 0000 0002 00AC 00AC"
        val (frequency, pattern) = ProntoHexConverter.convert(prontoHex)
        
        assertEquals(38000, frequency) // 38 * 1000
        assertTrue(pattern.isNotEmpty())
    }

    @Test
    fun testConvertNecProtocol() {
        // Typical NEC protocol Pronto Hex
        val prontoHex = "0000 006D 0000 0022 00AC 00AC 0015 0015 0015 0015 0015 0040 0015 0015 0015 0015"
        val (frequency, pattern) = ProntoHexConverter.convert(prontoHex)
        
        // Should convert to approximately 38kHz (0x6D = 109 decimal, but this is a frequency code)
        // Actually, 0x6D in frequency code context might be different
        assertTrue(frequency > 0)
        assertTrue(pattern.size >= 2)
    }

    @Test
    fun testPatternConversion() {
        val prontoHex = "0000 0026 0000 0002 00AC 00AC"
        val (_, pattern) = ProntoHexConverter.convert(prontoHex)
        
        // Should have mark and space
        assertEquals(2, pattern.size)
        assertTrue(pattern[0] > 0) // Mark
        assertTrue(pattern[1] > 0) // Space
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidFormatThrowsException() {
        ProntoHexConverter.convert("invalid format")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTooFewValuesThrowsException() {
        ProntoHexConverter.convert("0000 006D")
    }
}

