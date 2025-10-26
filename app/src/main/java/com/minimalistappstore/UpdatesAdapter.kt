package com.minimalistappstore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemUpdateBinding

typealias OnUpdateClickListener = (InstalledApp) -> Unit

class UpdatesAdapter(
    private val apps: List<InstalledApp>,
    private val onUpdateClick: OnUpdateClickListener
) : RecyclerView.Adapter<UpdatesAdapter.UpdateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val binding = ListItemUpdateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpdateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        holder.bind(apps[position], onUpdateClick)
    }

    override fun getItemCount(): Int = apps.size

    inner class UpdateViewHolder(private val binding: ListItemUpdateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: InstalledApp, onUpdateClick: OnUpdateClickListener) {
            binding.appIconImageView.load(app.iconUrl)
            binding.appNameTextView.text = app.name
            binding.updateStatusTextView.text = "Nova versão ${app.latestVersionName} disponível!"
            binding.updateButton.visibility = View.VISIBLE
            binding.updateButton.setOnClickListener {
                onUpdateClick(app)
            }
        }
    }
}