// DonationAdapter.kt
package com.minimalistappstore

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minimalistappstore.databinding.ListItemDonationBinding
import com.minimalistappstore.databinding.ItemDonationMethodBinding

class DonationAdapter(private val donationOptions: List<DonationOption>) :
    RecyclerView.Adapter<DonationAdapter.DonationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ListItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // CORREÇÃO CHAVE: Passamos o contexto do 'parent' para o ViewHolder
        return DonationViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(donationOptions[position])
    }

    override fun getItemCount(): Int = donationOptions.size

    inner class DonationViewHolder(private val binding: ListItemDonationBinding, private val context: Context) : RecyclerView.ViewHolder(binding.root) {

        fun bind(option: DonationOption) {
            binding.donationTitleTextView.text = option.title
            binding.donationSubtitleTextView.text = option.subtitle
            binding.donationDescriptionTextView.text = option.description

            binding.methodsContainer.removeAllViews()

            // CORREÇÃO CHAVE: Usamos a variável 'context' que recebemos no construtor
            val methodsAdapter = DonationMethodsAdapter(option.methods, context)
            binding.methodsContainer.addView(RecyclerView(context).apply {
                adapter = methodsAdapter
                layoutManager = LinearLayoutManager(context)
            })
        }
    }

    private class DonationMethodsAdapter(
        private val methods: List<DonationMethod>,
        private val context: Context
    ) : RecyclerView.Adapter<DonationMethodsAdapter.MethodViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MethodViewHolder {
            val binding = ItemDonationMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MethodViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MethodViewHolder, position: Int) {
            holder.bind(methods[position])
        }

        override fun getItemCount(): Int = methods.size

        inner class MethodViewHolder(private val binding: ItemDonationMethodBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(method: DonationMethod) {
                binding.methodNameTextView.text = method.name
                binding.methodValueTextView.text = method.value

                binding.copyButton.setOnClickListener {
                    Log.d("DonationAdapter", "Botão de copiar clicado para: ${method.name}")

                    val valueToCopy = if (method.name.contains("Wise", ignoreCase = true)) {
                        method.value.substringAfterLast("/")
                    } else {
                        method.value
                    }

                    // CORREÇÃO CHAVE: Usamos a variável 'context' aqui também
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val clip = ClipData.newPlainText("Donation Info", valueToCopy)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()

                    if (method.type == "link" && !method.name.contains("Wise", ignoreCase = true)) {
                        openLink(method.value)
                    }
                }
            }

            private fun openLink(url: String) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        }
    }
}