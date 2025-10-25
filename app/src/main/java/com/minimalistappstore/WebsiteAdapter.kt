// WebsiteAdapter.kt
package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemAppBinding

class WebsiteAdapter(private val websites: List<Website>) :
    RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsiteViewHolder {
        val binding = ListItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebsiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WebsiteViewHolder, position: Int) {
        holder.bind(websites[position])
    }

    override fun getItemCount(): Int = websites.size

    inner class WebsiteViewHolder(private val binding: ListItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(website: Website) {
            // Usando um ícone genérico de globo para sites
            binding.appIconImageView.load("https://i.imgur.com/GZg8M9x.png")
            binding.appNameTextView.text = website.name
            // Reaproveitamos o campo de desenvolvedor para a descrição
            binding.developerNameTextView.text = website.description

            binding.root.setOnClickListener {
                // Cria um Intent para abrir a URL no navegador
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website.url))
                it.context.startActivity(intent)
            }
        }
    }
}