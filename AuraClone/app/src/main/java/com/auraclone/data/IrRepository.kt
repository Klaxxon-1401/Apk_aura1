package com.auraclone.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class IrCode(
    val device: String,
    val protocol: String,
    val codes: Map<String, String>
)

class IrRepository(private val context: Context) {
    private var brandsData: Map<String, List<IrCode>>? = null

    suspend fun loadData() {
        if (brandsData != null) return

        withContext(Dispatchers.IO) {
            try {
                context.assets.open("irdb.json").use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<Map<String, List<IrCode>>>() {}.type
                    brandsData = Gson().fromJson(reader, type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                brandsData = emptyMap()
            }
        }
    }

    fun getBrands(): List<String> {
        return brandsData?.keys?.sorted() ?: emptyList()
    }

    fun getDevicesForBrand(brand: String): List<IrCode> {
        return brandsData?.get(brand) ?: emptyList()
    }
}
