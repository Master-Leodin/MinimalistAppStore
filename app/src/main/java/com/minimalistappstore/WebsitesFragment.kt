// WebsitesFragment.kt
package com.minimalistappstore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimalistappstore.databinding.FragmentWebsitesBinding
import kotlinx.coroutines.launch

class WebsitesFragment : Fragment() {

    private var _binding: FragmentWebsitesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebsitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadWebsites()
    }

    private fun setupRecyclerView() {
        binding.websitesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadWebsites() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = WebsiteFetcher.fetchWebsites()
            binding.progressBar.visibility = View.GONE
            result.onSuccess { websites ->
                if (websites.isEmpty()) {
                    Toast.makeText(requireContext(), "Nenhum site encontrado.", Toast.LENGTH_SHORT).show()
                } else {
                    val adapter = WebsiteAdapter(websites)
                    binding.websitesRecyclerView.adapter = adapter
                }
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Erro ao carregar sites: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}