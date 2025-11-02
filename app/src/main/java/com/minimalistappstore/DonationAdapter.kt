package com.minimalistappstore

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.minimalistappstore.databinding.ListItemDonationBinding
import com.minimalistappstore.databinding.ItemDonationMethodBinding

class DonationAdapter(private val donationOptions: List<DonationOption>) :
    RecyclerView.Adapter<DonationAdapter.DonationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ListItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

            val methodsAdapter = DonationMethodsAdapter(option.methods, context)
            val methodsRecyclerView = RecyclerView(context).apply {
                adapter = methodsAdapter
                layoutManager = LinearLayoutManager(context)
            }
            binding.methodsContainer.addView(methodsRecyclerView)
        }
    }

    private class DonationMethodsAdapter(
        private val methods: List<DonationMethod>,
        private val context: Context
    ) : RecyclerView.Adapter<DonationMethodsAdapter.MethodViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MethodViewHolder {
            val binding = ItemDonationMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MethodViewHolder(binding, context)
        }

        override fun onBindViewHolder(holder: MethodViewHolder, position: Int) {
            holder.bind(methods[position])
        }

        override fun getItemCount(): Int = methods.size

        inner class MethodViewHolder(
            private val binding: ItemDonationMethodBinding,
            private val context: Context
        ) : RecyclerView.ViewHolder(binding.root) {

            private var currentRetryCount = 0
            private val maxRetries = 2
            private val retryDelay = 2000L
            private val handler = Handler(Looper.getMainLooper())

            fun bind(method: DonationMethod) {
                loadMethodIconWithRetry(method.iconUrl)

                binding.methodNameTextView.text = method.name
                binding.methodValueTextView.text = method.value

                binding.copyButton.setOnClickListener {
                    Log.d("DonationAdapter", "Botão de copiar clicado para: ${method.name}")

                    val valueToCopy = if (method.name.contains("Wise", ignoreCase = true)) {
                        method.value.substringAfterLast("/")
                    } else {
                        method.value
                    }

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Donation Info", valueToCopy)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()

                    if (method.type == "link" && !method.name.contains("Wise", ignoreCase = true)) {
                        openLink(method.value)
                    }
                }
            }

            private fun loadMethodIconWithRetry(iconUrl: String) {
                binding.methodIconImageView.load(iconUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_help)
                    error(android.R.drawable.ic_menu_help)

                    listener(
                        onError = { _, result ->
                            if (currentRetryCount < maxRetries) {
                                currentRetryCount++
                                handler.postDelayed({
                                    loadMethodIconWithRetry(iconUrl)
                                }, retryDelay)
                            }
                        },
                        onSuccess = { _, _ ->
                            currentRetryCount = 0
                        }
                    )
                }
            }

            private fun openLink(url: String) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}