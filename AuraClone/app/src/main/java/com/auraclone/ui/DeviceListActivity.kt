package com.auraclone.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.auraclone.R
import com.auraclone.data.IrRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceListActivity : AppCompatActivity() {
    private lateinit var irRepository: IrRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private var brand: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brand_list) // Reuse layout

        brand = intent.getStringExtra("brand") ?: ""
        irRepository = IrRepository(this)

        // Update title
        findViewById<android.widget.TextView>(R.id.tv_title).text = brand

        // Initialize views
        recyclerView = findViewById(R.id.rv_brand_list)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.brand_sc_result)
        val progressBar = findViewById<View>(R.id.pb_loading)
        val loadingText = findViewById<View>(R.id.tv_loading)

        // Hide index bar for device list - REMOVED as it is gone from layout
        // findViewById<com.payne.okux.view.widget.IndexBar>(R.id.index_bar).visibility = View.GONE

        // Setup RecyclerView
        adapter = DeviceAdapter(brand) { device ->
            // Navigate to remote control
            val intent = Intent(this, RemoteControlActivity::class.java)
            intent.putExtra("brand", brand)
            intent.putExtra("device", device)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Setup search
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        // Load devices
        loadDevices(progressBar, loadingText)
    }

    private fun loadDevices(progressBar: View, loadingText: View) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                loadingText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }

                val devices = irRepository.getDevicesForBrand(brand)
                adapter.updateList(devices)

                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DeviceListActivity, "Error loading devices: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
            }
        }
    }
}

class DeviceAdapter(
    private val brand: String,
    private val onDeviceClick: (String) -> Unit
) : ListAdapter<String, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var fullList: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brand, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateList(list: List<String>) {
        fullList = list
        super.submitList(list)
    }

    fun filter(query: String) {
        val filtered = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter { it.contains(query, ignoreCase = true) }
        }
        super.submitList(filtered)
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_brand_name)

        fun bind(device: String) {
            textView.text = device
            itemView.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

