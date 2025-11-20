package com.auraclone.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.auraclone.R
import com.auraclone.data.IrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplianceSelectionActivity : AppCompatActivity() {
    private lateinit var irRepository: IrRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplianceAdapter
    private var brand: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brand_list) // Reuse layout, just change title

        brand = intent.getStringExtra("brand") ?: ""
        irRepository = IrRepository(this)

        // Update title
        findViewById<TextView>(R.id.tv_title).text = "$brand Devices"
        
        // Hide search view as we just show types
        findViewById<View>(R.id.brand_sc_result).visibility = View.GONE
        findViewById<View>(R.id.v_title_line).visibility = View.GONE

        // Initialize views
        recyclerView = findViewById(R.id.rv_brand_list)
        val progressBar = findViewById<View>(R.id.pb_loading)
        val loadingText = findViewById<View>(R.id.tv_loading)

        // Setup RecyclerView (Grid)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ApplianceAdapter { type ->
            // Navigate to Test Remote
            val intent = Intent(this, TestRemoteActivity::class.java)
            intent.putExtra("brand", brand)
            intent.putExtra("type", type)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Setup back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Load types
        loadTypes(progressBar, loadingText)
    }

    private fun loadTypes(progressBar: View, loadingText: View) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                loadingText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }

                val types = irRepository.getDeviceTypes(brand)
                adapter.submitList(types)

                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ApplianceSelectionActivity, "Error loading types: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
            }
        }
    }
}

class ApplianceAdapter(
    private val onTypeClick: (String) -> Unit
) : RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    private var types: List<String> = emptyList()

    fun submitList(list: List<String>) {
        types = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        holder.bind(types[position])
    }

    override fun getItemCount(): Int = types.size

    inner class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.iv_appliance_icon)
        private val nameView: TextView = itemView.findViewById(R.id.tv_appliance_name)

        fun bind(type: String) {
            nameView.text = type
            
            // Set icon based on type
            val iconRes = when (type) {
                "TV" -> R.mipmap.ic_launcher // Replace with TV icon if available
                "AC" -> R.mipmap.ic_launcher // Replace with AC icon
                "DVD" -> R.mipmap.ic_launcher
                "Set Top Box" -> R.mipmap.ic_launcher
                "Projector" -> R.mipmap.ic_launcher
                else -> R.mipmap.ic_launcher
            }
            // We use ic_launcher as placeholder for now
            // iconView.setImageResource(iconRes) 
            
            itemView.setOnClickListener {
                onTypeClick(type)
            }
        }
    }
}
