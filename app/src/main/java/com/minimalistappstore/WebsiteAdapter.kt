// WebsiteAdapter.kt
package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemWebsiteBinding // Novo binding

class WebsiteAdapter(private val websites: List<Website>) :
    RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsiteViewHolder {
        val binding = ListItemWebsiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebsiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WebsiteViewHolder, position: Int) {
        holder.bind(websites[position])
    }

    override fun getItemCount(): Int = websites.size

    inner class WebsiteViewHolder(private val binding: ListItemWebsiteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(website: Website) {
            binding.websiteIconImageView.load(website.iconUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_websites)
                error(R.drawable.ic_websites)
            }

            binding.websiteNameTextView.text = website.name
            binding.websiteDescriptionTextView.text = website.description

            binding.root.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website.url))
                it.context.startActivity(intent)
            }
        }
    }
}