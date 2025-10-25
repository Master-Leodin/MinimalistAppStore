// MainActivity.kt
package com.minimalistappstore

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimalistappstore.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadApps() {
        // Mostra um indicador de carregamento (opcional, mas bom)
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = AppFetcher.fetchApps()
            binding.progressBar.visibility = View.GONE // Esconde o carregamento

            result.onSuccess { apps ->
                val adapter = AppAdapter(this@MainActivity, apps)
                binding.appsRecyclerView.adapter = adapter
            }.onFailure { error ->
                // Mostra uma mensagem de erro em caso de falha na rede
                Toast.makeText(this@MainActivity, "Erro ao carregar apps: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}