package com.minimalistappstore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimalistappstore.databinding.FragmentUpdatesBinding
import kotlinx.coroutines.launch

class UpdatesFragment : Fragment() {

    private var _binding: FragmentUpdatesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkForUpdates()
    }

    private fun checkForUpdates() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = UpdatesFetcher.checkForUpdates(requireContext())
            binding.progressBar.visibility = View.GONE
            result.onSuccess { apps ->
                if (apps.isEmpty()) {
                    Toast.makeText(requireContext(), "Todos os apps estão atualizados.", Toast.LENGTH_SHORT).show()
                } else {
                    val adapter = UpdatesAdapter(apps) { app -> openAppDetail(app) }
                    binding.updatesRecyclerView.adapter = adapter
                }
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Erro ao verificar atualizações.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openAppDetail(app: InstalledApp) {
        val intent = Intent(requireContext(), AppDetailActivity::class.java).apply {
            putExtra("APP_EXTRA", App(
                name = app.name,
                developer = app.developer,
                description = app.description,
                iconUrl = app.iconUrl,
                apkUrl = app.apkUrl,
                version = app.version,
                openSourceUrl = app.openSourceUrl,
                packageName = app.packageName
            ))
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}