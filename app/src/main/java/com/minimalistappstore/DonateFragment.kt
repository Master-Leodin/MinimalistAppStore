package com.minimalistappstore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimalistappstore.databinding.FragmentDonateBinding
import kotlinx.coroutines.launch

class DonateFragment : Fragment() {

    private var _binding: FragmentDonateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadDonations()
    }

    private fun setupRecyclerView() {
        // Esta linha é crucial para evitar o erro "No layout manager attached"
        binding.donationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadDonations() {
        Log.d("DonateFragment", "Iniciando carregamento das doações.")
        // Mostra a barra de progresso
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = DonationFetcher.fetchDonations()
            // Esconde a barra de progresso
            binding.progressBar.visibility = View.GONE

            result.onSuccess { donations ->
                Log.d("DonateFragment", "Doações carregadas com sucesso! Quantidade: ${donations.size}")
                if (donations.isNotEmpty()) {
                    val adapter = DonationAdapter(donations)
                    binding.donationsRecyclerView.adapter = adapter
                    Log.d("DonateFragment", "Adapter definido no RecyclerView.")
                } else {
                    Log.w("DonateFragment", "A lista de doações está vazia.")
                    Toast.makeText(requireContext(), "Nenhuma doação encontrada.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                // Este log é muito importante!
                Log.e("DonateFragment", "Falha ao carregar doações.", error)
                Toast.makeText(requireContext(), "Erro ao carregar doações: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}