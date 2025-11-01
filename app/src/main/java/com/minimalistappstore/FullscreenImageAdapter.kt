package com.minimalistappstore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ItemFullscreenImageBinding

class FullscreenImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenImageViewHolder {
        val binding = ItemFullscreenImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FullscreenImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FullscreenImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size

    inner class FullscreenImageViewHolder(private val binding: ItemFullscreenImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            binding.fullscreenImage.load(imageUrl) {
                crossfade(true)
                error(R.drawable.screenshot_placeholder)
            }

            // Fecha ao clicar na imagem (se estiver usando o layout simples)
            binding.fullscreenImage.setOnClickListener {
                // O dismiss é tratado no dialog
            }
        }
    }
}