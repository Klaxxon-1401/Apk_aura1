package com.auraclone.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.auraclone.R
import com.auraclone.data.IrRepository
import com.auraclone.ir.IrCodeTransmitter
import com.auraclone.ir.IrManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteControlActivity : AppCompatActivity() {
    private lateinit var irRepository: IrRepository
    private lateinit var irCodeTransmitter: IrCodeTransmitter
    private var brand: String = ""
    private var device: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.remote_fragment)

        brand = intent.getStringExtra("brand") ?: ""
        device = intent.getStringExtra("device") ?: ""

        irRepository = IrRepository(this)
        irCodeTransmitter = IrCodeTransmitter(IrManager(this), irRepository)

        // Update title
        findViewById<TextView>(R.id.tv_title)?.text = "$brand $device"

        // Setup back button
        findViewById<View>(R.id.iv_back)?.setOnClickListener {
            finish()
        }

        // Load and display remote buttons
        // loadRemoteButtons()
    }

    private fun loadRemoteButtons() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }

                val functions = irRepository.getFunctionsForDevice(brand, device)
                // displayButtons(functions)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RemoteControlActivity, "Error loading functions: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayButtons(functions: List<String>) {
        // val gridLayout = findViewById<GridLayout>(R.id.remote_grid) ?: return
        // gridLayout.removeAllViews()

        // // Add buttons for each function
        // functions.forEach { function ->
        //     val button = Button(this).apply {
        //         text = function
        //         layoutParams = GridLayout.LayoutParams().apply {
        //             width = 0
        //             height = GridLayout.LayoutParams.WRAP_CONTENT
        //             columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        //             rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        //             setMargins(8, 8, 8, 8)
        //         }
        //         setOnClickListener {
        //             transmitCode(function)
        //         }
        //     }
        //     gridLayout.addView(button)
        // }
    }

    private fun transmitCode(function: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    irCodeTransmitter.transmit(brand, device, function)
                }
                Toast.makeText(this@RemoteControlActivity, "Sent: $function", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RemoteControlActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

