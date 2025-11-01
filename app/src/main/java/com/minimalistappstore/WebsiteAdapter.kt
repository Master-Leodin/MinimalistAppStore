package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemAppBinding

class WebsiteAdapter(private val websites: List<Website>) :
    RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsiteViewHolder {
        val binding = ListItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebsiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WebsiteViewHolder, position: Int) {
        holder.bind(websites[position])
    }

    override fun getItemCount(): Int = websites.size

    inner class WebsiteViewHolder(private val binding: ListItemAppBinding) : RecyclerView.ViewHolder(binding.root) {

        private var currentRetryCount = 0
        private val maxRetries = 2
        private val retryDelay = 2000L

        fun bind(website: Website) {
            loadWebsiteIconWithRetry(website.iconUrl)

            binding.appNameTextView.text = website.name
            binding.developerNameTextView.text = website.description

            binding.root.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website.url))
                it.context.startActivity(intent)
            }
        }

        private fun loadWebsiteIconWithRetry(iconUrl: String) {
            binding.appIconImageView.load(iconUrl) {
                crossfade(true)
                // Placeholder genérico para websites
                placeholder(R.drawable.ic_websites)
                // Fallback se não carregar
                error(R.drawable.ic_websites)

                listener(
                    onError = { _, result ->
                        if (currentRetryCount < maxRetries) {
                            currentRetryCount++
                            handler.postDelayed({
                                loadWebsiteIconWithRetry(iconUrl)
                            }, retryDelay)
                        }
                    },
                    onSuccess = { _, _ ->
                        currentRetryCount = 0 // Reset no sucesso
                    }
                )
            }
        }
    }
}