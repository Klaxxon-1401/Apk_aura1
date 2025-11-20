package com.auraclone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.auraclone.R

class BrandAdapter(
    private val onBrandClick: (String) -> Unit
) : ListAdapter<String, BrandAdapter.BrandViewHolder>(BrandDiffCallback()) {

    private var fullList: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brand, parent, false)
        return BrandViewHolder(view)
    }

    override fun onBindViewHolder(holder: BrandViewHolder, position: Int) {
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

    inner class BrandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_brand_name)

        fun bind(brand: String) {
            textView.text = brand
            itemView.setOnClickListener {
                onBrandClick(brand)
            }
        }
    }

    class BrandDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

