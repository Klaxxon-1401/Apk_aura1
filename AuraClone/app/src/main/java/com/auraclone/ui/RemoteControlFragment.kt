package com.auraclone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.auraclone.R
import com.auraclone.data.IrRepository
import com.auraclone.ir.IrCodeTransmitter
import com.auraclone.ir.IrManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteControlFragment : Fragment() {
    private lateinit var irRepository: IrRepository
    private lateinit var irCodeTransmitter: IrCodeTransmitter
    private var brand: String = ""
    private var device: String = ""

    companion object {
        private const val ARG_BRAND = "brand"
        private const val ARG_DEVICE = "device"

        fun newInstance(brand: String, device: String): RemoteControlFragment {
            val fragment = RemoteControlFragment()
            val args = Bundle().apply {
                putString(ARG_BRAND, brand)
                putString(ARG_DEVICE, device)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            brand = it.getString(ARG_BRAND, "")
            device = it.getString(ARG_DEVICE, "")
        }
        irRepository = IrRepository(requireContext())
        irCodeTransmitter = IrCodeTransmitter(IrManager(requireContext()), irRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use TV fragment layout as base - can be customized per device type
        return inflater.inflate(R.layout.fragment_tv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup all button click listeners
        setupButtons(view)
        
        // Load functions and map them to buttons
        loadFunctions()
    }

    private fun setupButtons(view: View) {
        // Power button
        view.findViewById<ImageView>(R.id.iv_power)?.setOnClickListener {
            transmitCode("Power")
        }

        // Directional pad
        view.findViewById<ImageView>(R.id.iv_key_up)?.setOnClickListener {
            transmitCode("Up")
        }
        view.findViewById<ImageView>(R.id.iv_key_down)?.setOnClickListener {
            transmitCode("Down")
        }
        view.findViewById<ImageView>(R.id.iv_key_left)?.setOnClickListener {
            transmitCode("Left")
        }
        view.findViewById<ImageView>(R.id.iv_key_right)?.setOnClickListener {
            transmitCode("Right")
        }
        view.findViewById<ImageView>(R.id.iv_key_ok)?.setOnClickListener {
            transmitCode("OK") ?: transmitCode("Select") ?: transmitCode("Enter")
        }

        // Volume controls
        view.findViewById<ImageButton>(R.id.iv_vol_up)?.setOnClickListener {
            transmitCode("Volume+") ?: transmitCode("Volume Up")
        }
        view.findViewById<ImageButton>(R.id.iv_vol_down)?.setOnClickListener {
            transmitCode("Volume-") ?: transmitCode("Volume Down")
        }

        // Channel controls
        view.findViewById<ImageView>(R.id.iv_channel_up)?.setOnClickListener {
            transmitCode("Channel+") ?: transmitCode("Channel Up")
        }
        view.findViewById<ImageView>(R.id.iv_channel_down)?.setOnClickListener {
            transmitCode("Channel-") ?: transmitCode("Channel Down")
        }

        // Other buttons
        view.findViewById<TextView>(R.id.tv_tv_av)?.setOnClickListener {
            transmitCode("AV") ?: transmitCode("Input")
        }
        view.findViewById<TextView>(R.id.tv_menu)?.setOnClickListener {
            transmitCode("Menu")
        }
        view.findViewById<TextView>(R.id.tv_i)?.setOnClickListener {
            transmitCode("Back") ?: transmitCode("Return")
        }
        view.findViewById<ImageView>(R.id.iv_mute)?.setOnClickListener {
            transmitCode("Mute")
        }
        view.findViewById<TextView>(R.id.tv_123)?.setOnClickListener {
            transmitCode("123") ?: transmitCode("Number Pad")
        }
        view.findViewById<TextView>(R.id.tv_quit)?.setOnClickListener {
            transmitCode("Exit") ?: transmitCode("Quit")
        }
        view.findViewById<ImageView>(R.id.iv_more)?.setOnClickListener {
            // Show more functions dialog
            showMoreFunctions()
        }
    }

    private fun loadFunctions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }
                // Functions are loaded, buttons are already set up
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun transmitCode(function: String): Boolean {
        return try {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Try exact match first, then try variations
                    val code = irRepository.getCode(brand, device, function)
                        ?: irRepository.getCode(brand, device, findFunctionVariation(function))
                    
                    if (code != null) {
                        irCodeTransmitter.transmit(code)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Sent: $function", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Function not found: $function", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun findFunctionVariation(function: String): String {
        // Map common button names to IRDB function names
        val variations = mapOf(
            "Power" to listOf("Power", "On", "Off", "Power On", "Power Off"),
            "Up" to listOf("Up", "Arrow Up", "Up Arrow"),
            "Down" to listOf("Down", "Arrow Down", "Down Arrow"),
            "Left" to listOf("Left", "Arrow Left", "Left Arrow"),
            "Right" to listOf("Right", "Arrow Right", "Right Arrow"),
            "OK" to listOf("OK", "Select", "Enter", "OK/Select"),
            "Select" to listOf("Select", "OK", "Enter"),
            "Enter" to listOf("Enter", "OK", "Select"),
            "Volume+" to listOf("Volume+", "Volume Up", "Vol+", "Volume Up"),
            "Volume Up" to listOf("Volume Up", "Volume+", "Vol+"),
            "Volume-" to listOf("Volume-", "Volume Down", "Vol-", "Volume Down"),
            "Volume Down" to listOf("Volume Down", "Volume-", "Vol-"),
            "Channel+" to listOf("Channel+", "Channel Up", "Ch+", "Channel Up"),
            "Channel Up" to listOf("Channel Up", "Channel+", "Ch+"),
            "Channel-" to listOf("Channel-", "Channel Down", "Ch-", "Channel Down"),
            "Channel Down" to listOf("Channel Down", "Channel-", "Ch-"),
            "AV" to listOf("AV", "Input", "Source", "TV/AV"),
            "Input" to listOf("Input", "AV", "Source"),
            "Menu" to listOf("Menu"),
            "Back" to listOf("Back", "Return", "Exit"),
            "Return" to listOf("Return", "Back", "Exit"),
            "Mute" to listOf("Mute"),
            "123" to listOf("123", "Number Pad", "Numbers"),
            "Number Pad" to listOf("Number Pad", "123", "Numbers"),
            "Exit" to listOf("Exit", "Quit", "Back"),
            "Quit" to listOf("Quit", "Exit", "Back")
        )
        
        return variations[function]?.firstOrNull() ?: function
    }

    private fun showMoreFunctions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val functions = withContext(Dispatchers.IO) {
                    irRepository.getFunctionsForDevice(brand, device)
                }
                
                // Show dialog with all available functions
                // For now, just show a toast
                Toast.makeText(requireContext(), "Available functions: ${functions.joinToString(", ")}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

