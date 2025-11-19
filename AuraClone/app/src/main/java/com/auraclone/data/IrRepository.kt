package com.auraclone.data

import android.content.Context
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * IR Code data structure matching probonopd/irdb format
 */
data class IrCode(
    val device: String,
    val protocol: String,
    val function: String,
    val prontoHex: String? = null,
    val rawFrequency: Int? = null,
    val rawPattern: IntArray? = null
) {
    /**
     * Get frequency and pattern, converting from Pronto Hex if needed
     */
    fun getFrequencyAndPattern(): Pair<Int, IntArray> {
        return when {
            rawFrequency != null && rawPattern != null -> {
                Pair(rawFrequency!!, rawPattern!!)
            }
            prontoHex != null && ProntoHexConverter.isValidProntoHex(prontoHex) -> {
                ProntoHexConverter.convert(prontoHex)
            }
            else -> {
                // Default fallback (NEC protocol example)
                Pair(38000, intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560, 560, 560))
            }
        }
    }
}

/**
 * Repository for loading and querying IR database from probonopd/irdb
 * Supports both CSV format (from probonopd/irdb) and JSON format (legacy)
 */
class IrRepository(private val context: Context) {
    private var brandsData: Map<String, List<IrCode>>? = null
    private var devicesByBrand: Map<String, List<String>>? = null
    private var functionsByDevice: Map<String, List<String>>? = null

    /**
     * Load IR database from assets folder
     * Supports both CSV (probonopd/irdb format) and JSON (legacy format)
     */
    suspend fun loadData() {
        if (brandsData != null) return

        withContext(Dispatchers.IO) {
            try {
                // Try CSV format first (probonopd/irdb)
                val csvFiles = listOf("irdb.csv", "irdb_data.csv")
                var loaded = false
                
                // Try each CSV file until one succeeds
                for (csvFile in csvFiles) {
                    try {
                        context.assets.open(csvFile).use { inputStream ->
                            loadFromCsv(InputStreamReader(inputStream))
                            loaded = true
                            return@withContext // Exit early on success
                        }
                    } catch (e: Exception) {
                        // File doesn't exist, try next - continue to next iteration
                        // (no action needed, loop will continue)
                    }
                }
                
                // Fallback to JSON format if CSV not found
                if (!loaded) {
                    try {
                        context.assets.open("irdb.json").use { inputStream ->
                            loadFromJson(InputStreamReader(inputStream))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        brandsData = emptyMap()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                brandsData = emptyMap()
            }
        }
    }

    /**
     * Load data from CSV format (probonopd/irdb)
     * Expected columns: Brand, Device, Function, Protocol, Pronto Hex
     */
    private fun loadFromCsv(reader: InputStreamReader) {
        val csvReader = CSVReaderBuilder(reader)
            .withSkipLines(1) // Skip header
            .build()
        
        val codesByBrand = mutableMapOf<String, MutableList<IrCode>>()
        val deviceMap = mutableMapOf<String, MutableList<String>>()
        val functionMap = mutableMapOf<String, MutableList<String>>()
        
        csvReader.use { reader ->
            reader.readAll().forEach { row ->
                if (row.size >= 4) {
                    val brand = row[0].trim()
                    val device = row[1].trim()
                    val function = row[2].trim()
                    val protocol = if (row.size > 3) row[3].trim() else "Unknown"
                    val prontoHex = if (row.size > 4) row[4].trim().takeIf { it.isNotEmpty() } else null
                    
                    val code = IrCode(
                        device = device,
                        protocol = protocol,
                        function = function,
                        prontoHex = prontoHex
                    )
                    
                    codesByBrand.getOrPut(brand) { mutableListOf() }.add(code)
                    
                    // Build device and function maps
                    val brandDeviceKey = "$brand|$device"
                    deviceMap.getOrPut(brand) { mutableListOf() }.apply {
                        if (!contains(device)) add(device)
                    }
                    functionMap.getOrPut(brandDeviceKey) { mutableListOf() }.apply {
                        if (!contains(function)) add(function)
                    }
                }
            }
        }
        
        brandsData = codesByBrand
        devicesByBrand = deviceMap
        functionsByDevice = functionMap
    }

    /**
     * Load data from JSON format (legacy)
     */
    private fun loadFromJson(reader: InputStreamReader) {
        // Legacy JSON format support
        // This would parse the old JSON structure if needed
        // For now, we'll focus on CSV format
        brandsData = emptyMap()
    }

    /**
     * Get all available brands
     */
    fun getBrands(): List<String> {
        return brandsData?.keys?.sorted() ?: emptyList()
    }

    /**
     * Get all devices for a specific brand
     */
    fun getDevicesForBrand(brand: String): List<String> {
        return devicesByBrand?.get(brand)?.sorted() ?: emptyList()
    }

    /**
     * Get all functions for a specific brand and device
     */
    fun getFunctionsForDevice(brand: String, device: String): List<String> {
        val key = "$brand|$device"
        return functionsByDevice?.get(key)?.sorted() ?: emptyList()
    }

    /**
     * Get IR code for a specific brand, device, and function
     */
    fun getCode(brand: String, device: String, function: String): IrCode? {
        return brandsData?.get(brand)?.firstOrNull { 
            it.device.equals(device, ignoreCase = true) && 
            it.function.equals(function, ignoreCase = true)
        }
    }

    /**
     * Search for devices by name (case-insensitive partial match)
     */
    fun searchDevices(query: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val queryLower = query.lowercase()
        
        brandsData?.forEach { (brand, codes) ->
            codes.forEach { code ->
                if (code.device.lowercase().contains(queryLower)) {
                    results.add(Pair(brand, code.device))
                }
            }
        }
        
        return results.distinct()
    }
}
