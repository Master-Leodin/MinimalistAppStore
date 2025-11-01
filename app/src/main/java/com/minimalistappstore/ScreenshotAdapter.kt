package com.minimalistappstore

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ItemScreenshotBinding

typealias OnScreenshotClickListener = (String, Int) -> Unit

class ScreenshotAdapter(
    private val screenshotUrls: List<String>,
    private val onScreenshotClick: OnScreenshotClickListener
) : RecyclerView.Adapter<ScreenshotAdapter.ScreenshotViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotViewHolder {
        val binding = ItemScreenshotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScreenshotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScreenshotViewHolder, position: Int) {
        holder.bind(screenshotUrls[position], position)
    }

    override fun getItemCount(): Int = screenshotUrls.size

    override fun onViewRecycled(holder: ScreenshotViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    inner class ScreenshotViewHolder(private val binding: ItemScreenshotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentRetryCount = 0
        private val maxRetries = 3
        private val retryDelay = 3000L
        private var currentImageUrl: String? = null
        private var retryRunnable: Runnable? = null

        fun bind(imageUrl: String, position: Int) {
            currentImageUrl = imageUrl
            currentRetryCount = 0

            // Configura o clique na imagem
            binding.screenshotImageView.setOnClickListener {
                onScreenshotClick(imageUrl, position)
            }

            loadImageWithRetry(imageUrl)
        }

        private fun loadImageWithRetry(imageUrl: String) {
            binding.loadingProgressBar.visibility = android.view.View.VISIBLE

            binding.screenshotImageView.load(imageUrl) {
                crossfade(true)
                error(R.drawable.screenshot_placeholder)
                listener(
                    onStart = {
                        binding.loadingProgressBar.visibility = android.view.View.VISIBLE
                    },
                    onSuccess = { _, _ ->
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        currentRetryCount = 0
                        retryRunnable = null
                    },
                    onError = { _, _ ->
                        binding.loadingProgressBar.visibility = android.view.View.GONE

                        if (currentRetryCount < maxRetries) {
                            currentRetryCount++
                            retryRunnable = Runnable {
                                if (currentImageUrl == imageUrl) {
                                    binding.loadingProgressBar.visibility = android.view.View.VISIBLE
                                    loadImageWithRetry(imageUrl)
                                }
                            }
                            handler.postDelayed(retryRunnable!!, retryDelay)
                        } else {
                            binding.screenshotImageView.setImageResource(R.drawable.screenshot_placeholder)
                            currentRetryCount = 0
                            retryRunnable = null
                        }
                    }
                )
            }
        }

        fun clear() {
            retryRunnable?.let { handler.removeCallbacks(it) }
            retryRunnable = null
            currentRetryCount = 0
            currentImageUrl = null
            binding.screenshotImageView.setOnClickListener(null)
        }
    }
}