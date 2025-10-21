// AppAdapter.kt
package com.minimalistappstore

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemAppBinding

class AppAdapter(private val context: Context, private val apps: List<App>) :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ListItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(private val binding: ListItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: App) {
            binding.appIconImageView.load(app.iconUrl)
            binding.appNameTextView.text = app.name
            binding.developerNameTextView.text = app.developer

            binding.root.setOnClickListener {
                val intent = Intent(context, AppDetailActivity::class.java)
                intent.putExtra("APP_EXTRA", app) // Passa o objeto App inteiro
                context.startActivity(intent)
            }
        }
    }
}