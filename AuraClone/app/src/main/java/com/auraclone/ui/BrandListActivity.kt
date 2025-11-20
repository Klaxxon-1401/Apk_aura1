package com.auraclone.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.auraclone.R
import com.auraclone.data.IrRepository
import com.payne.okux.view.widget.IndexBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrandListActivity : AppCompatActivity() {
    private lateinit var irRepository: IrRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BrandAdapter
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var progressBar: View
    private lateinit var loadingText: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brand_list)

        irRepository = IrRepository(this)
        
        // Initialize views
        recyclerView = findViewById(R.id.rv_brand_list)
        searchView = findViewById(R.id.brand_sc_result)
        progressBar = findViewById(R.id.pb_loading)
        loadingText = findViewById(R.id.tv_loading)

        // Setup RecyclerView
        adapter = BrandAdapter { brand ->
            // Navigate to appliance selection
            val intent = Intent(this, ApplianceSelectionActivity::class.java)
            intent.putExtra("brand", brand)
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
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        // Load data
        loadBrands()
    }

    private fun loadBrands() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                loadingText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE


                withContext(Dispatchers.IO) {
                    irRepository.loadData()
                }

                val brands = irRepository.getBrands()
                adapter.updateList(brands)

                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE


            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@BrandListActivity, "Error loading brands: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
            }
        }
    }
}

