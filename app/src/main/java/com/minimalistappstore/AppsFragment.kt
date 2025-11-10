// AppsFragment.kt
package com.minimalistappstore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimalistappstore.databinding.FragmentAppsBinding
import kotlinx.coroutines.launch

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadApps() {
        // CORREÇÃO: Usar viewLifecycleOwner para evitar crashes
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AppFetcher.fetchApps()
            result.onSuccess { apps ->
                // CORREÇÃO: Verificar se o binding ainda está vivo
                if (_binding != null) {
                    val adapter = AppAdapter(requireContext(), apps)
                    binding.appsRecyclerView.adapter = adapter
                }
            }.onFailure { error ->
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Erro ao carregar apps: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}