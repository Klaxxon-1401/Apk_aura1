package com.auraclone.data

import android.content.Context
import com.auraclone.ir.IrProtocolConverter
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * IR Code data structure matching probonopd/irdb format
 */
data class IrCode(
    val device: String,
    val protocol: String,
    val function: String,
    val prontoHex: String? = null,
    val protocolCode: String? = null,
    val rawFrequency: Int? = null,
    val rawPattern: IntArray? = null
) {
    /**
     * Get frequency and pattern, converting from Pronto Hex or Protocol Code if needed
     */
    fun getFrequencyAndPattern(): Pair<Int, IntArray> {
        return when {
            rawFrequency != null && rawPattern != null -> {
                Pair(rawFrequency!!, rawPattern!!)
            }
            prontoHex != null && ProntoHexConverter.isValidProntoHex(prontoHex) -> {
                ProntoHexConverter.convert(prontoHex)
            }
            protocolCode != null -> {
                IrProtocolConverter.convert(protocolCode) ?: getDefaultPattern()
            }
            else -> {
                getDefaultPattern()
            }
        }
    }
    
    private fun getDefaultPattern(): Pair<Int, IntArray> {
        // Default fallback (NEC protocol example)
        return Pair(38000, intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560, 560, 560))
    }
}

/**
 * Repository for loading and querying IR database from probonopd/irdb
 * Supports both CSV format (from probonopd/irdb) and JSON format
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
                        // File doesn't exist, try next
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
     * Load data from JSON format
     */
    private fun loadFromJson(reader: InputStreamReader) {
        val jsonString = BufferedReader(reader).use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        
        val codesByBrand = mutableMapOf<String, MutableList<IrCode>>()
        val deviceMap = mutableMapOf<String, MutableList<String>>()
        val functionMap = mutableMapOf<String, MutableList<String>>()
        
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val rawBrand = keys.next()
            
            // Clean up brand name
            // Remove "Unknown_" prefix and underscores
            var cleanBrand = rawBrand.replace("Unknown_", "", ignoreCase = true)
                .replace("_", " ")
                .trim()
            
            // Capitalize first letter
            if (cleanBrand.isNotEmpty()) {
                cleanBrand = cleanBrand.substring(0, 1).uppercase() + cleanBrand.substring(1)
            }

            // Filter out invalid brands
            // Must start with a letter and be at least 2 characters long
            if (cleanBrand.isEmpty() || 
                cleanBrand.length < 2 || 
                !cleanBrand[0].isLetter()) {
                continue
            }
            
            val devicesArray = jsonObject.optJSONArray(rawBrand) ?: continue
            
            for (i in 0 until devicesArray.length()) {
                val deviceObj = devicesArray.optJSONObject(i) ?: continue
                val device = deviceObj.optString("device", "Unknown")
                val protocol = deviceObj.optString("protocol", "Unknown")
                val codesObj = deviceObj.optJSONObject("codes") ?: continue
                
                val functions = codesObj.keys()
                while (functions.hasNext()) {
                    val function = functions.next()
                    val codeString = codesObj.optString(function)
                    
                    // Skip metadata keys like "functionname"
                    if (function.equals("functionname", ignoreCase = true)) continue
                    
                    val code = IrCode(
                        device = device,
                        protocol = protocol,
                        function = function,
                        protocolCode = codeString
                    )
                    
                    codesByBrand.getOrPut(cleanBrand) { mutableListOf() }.add(code)
                    
                    // Build device and function maps
                    val brandDeviceKey = "$cleanBrand|$device"
                    deviceMap.getOrPut(cleanBrand) { mutableListOf() }.apply {
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

    /**
     * Get available device types for a brand (TV, AC, etc.)
     */
    fun getDeviceTypes(brand: String): List<String> {
        val codes = brandsData?.get(brand) ?: return emptyList()
        
        // Group by device ID
        val devices = codes.groupBy { it.device }
        
        // Infer type for each device
        return devices.values.map { deviceCodes ->
            inferDeviceType(deviceCodes)
        }.distinct().sorted()
    }

    /**
     * Get all device IDs for a specific brand and type
     */
    fun getDeviceIdsForBrandAndType(brand: String, type: String): List<String> {
        val codes = brandsData?.get(brand) ?: return emptyList()
        val devices = codes.groupBy { it.device }
        
        return devices.filter { (_, deviceCodes) ->
            inferDeviceType(deviceCodes) == type
        }.keys.sorted()
    }

    /**
     * Infer device type based on available functions
     */
    private fun inferDeviceType(codes: List<IrCode>): String {
        // Check device name first
        val name = codes.firstOrNull()?.device?.uppercase() ?: ""
        if (name.contains("TV")) return "TV"
        if (name.contains("AC") || name.contains("AIR")) return "AC"
        if (name.contains("DVD") || name.contains("BLURAY") || name.contains("BD")) return "DVD"
        if (name.contains("PROJECTOR")) return "Projector"
        if (name.contains("SAT") || name.contains("CABLE") || name.contains("STB")) return "Set Top Box"
        
        // Check functions
        val functions = codes.map { it.function.uppercase() }
        
        if (functions.any { it.contains("TEMP") || it.contains("FAN") || it.contains("COOL") }) return "AC"
        if (functions.any { it.contains("CH+") || it.contains("CHANNEL") || it.contains("MENU") }) return "TV"
        if (functions.any { it.contains("PLAY") || it.contains("PAUSE") || it.contains("EJECT") }) return "DVD"
        if (functions.any { it.contains("INPUT") || it.contains("SOURCE") }) return "TV" // Fallback for TV
        
        return "Other"
    }
}
