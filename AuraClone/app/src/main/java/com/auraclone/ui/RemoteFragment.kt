package com.auraclone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class RemoteFragment : Fragment() {
    private lateinit var irRepository: IrRepository
    private lateinit var irCodeTransmitter: IrCodeTransmitter

    companion object {
        fun newInstance(): RemoteFragment {
            return RemoteFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irRepository = IrRepository(requireContext())
        irCodeTransmitter = IrCodeTransmitter(IrManager(requireContext()), irRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.remote_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load data
        loadRemotes()
    }

    private fun loadRemotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading remotes: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
