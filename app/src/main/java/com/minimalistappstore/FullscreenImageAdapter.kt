package com.minimalistappstore

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ItemFullscreenImageBinding

class FullscreenImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {

    private val TAG = "FullscreenImageAdapter"

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
            Log.d(TAG, "Carregando imagem: $imageUrl")

            binding.fullscreenImage.load(imageUrl) {
                crossfade(true)
                // Usar um placeholder enquanto carrega
                placeholder(android.R.drawable.ic_menu_gallery)
                // Usar um ícone de erro visível
                error(android.R.drawable.ic_dialog_alert)
                listener(
                    onStart = {
                        Log.d(TAG, "Iniciando carregamento: $imageUrl")
                    },
                    onSuccess = { _, _ ->
                        Log.d(TAG, "Imagem carregada com sucesso: $imageUrl")
                    },
                    onError = { _, result ->
                        Log.e(TAG, "Erro ao carregar imagem: $imageUrl - ${result.throwable.message}")
                        // Forçar um ícone de erro visível
                        binding.fullscreenImage.setImageResource(android.R.drawable.ic_dialog_alert)
                    }
                )
            }
        }
    }
}