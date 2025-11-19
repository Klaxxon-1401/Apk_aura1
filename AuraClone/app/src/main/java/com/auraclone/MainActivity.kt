package com.auraclone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.auraclone.data.IrRepository
import com.auraclone.ir.IrManager

class MainActivity : AppCompatActivity() {
    private val irRepository by lazy { IrRepository(this) }
    private val irManager by lazy { IrManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<android.widget.TextView>(R.id.status_text) // Need to add ID to layout
        
        lifecycleScope.launch {
            statusText.text = "Loading IRDB..."
            irRepository.loadData()
            val brandCount = irRepository.getBrands().size
            
            val transmitter = irManager.getActiveTransmitter()
            val transmitterName = transmitter.getName()
            val hasEmitter = transmitter.hasIrEmitter()

            statusText.text = """
                IRDB Loaded: $brandCount brands
                Active Transmitter: $transmitterName
                Has Emitter: $hasEmitter
            """.trimIndent()

            findViewById<android.widget.Button>(R.id.test_button).setOnClickListener {
                // Simple NEC Power Code (Example)
                // Frequency: 38kHz
                // Pattern: Header Mark (9000), Header Space (4500), etc.
                // Just a dummy pattern for testing audio generation/API call
                val frequency = 38000
                val pattern = intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560, 560, 560)
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        irManager.transmit(frequency, pattern)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(this@MainActivity, "Signal Transmitted via $transmitterName", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(this@MainActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
