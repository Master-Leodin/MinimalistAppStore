package com.minimalistappstore

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
        Log.d("UpdatesFragment", "=== FRAGMENT DE ATUALIZAÇÕES INICIADO ===")

        debugInstalledApps()
        DebugHelper.debugEverything(requireContext())

        checkForUpdates()

        // Clique longo na progress bar para forçar registro
        binding.progressBar.setOnLongClickListener {
            Log.d("UpdatesFragment", "🔄 Forçando registro manual...")
            debugForceRegistration()
            checkForUpdates()
            true
        }
    }

    private fun debugInstalledApps() {
        val prefs = requireContext().getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val allEntries = prefs.all

        Log.d("UpdatesFragment", "=== DEBUG MANUAL DO SHAREDPREFERENCES ===")
        if (allEntries.isEmpty()) {
            Log.d("UpdatesFragment", "❌ NENHUM APP REGISTRADO NO SHAREDPREFERENCES!")
        } else {
            Log.d("UpdatesFragment", "✅ ${allEntries.size} app(s) registrado(s):")
            for ((key, value) in allEntries) {
                Log.d("UpdatesFragment", "📱 $key -> $value")

                try {
                    val packageInfo = requireContext().packageManager.getPackageInfo(key, 0)
                    Log.d("UpdatesFragment", "   ✅ INSTALADO - VersionCode: ${packageInfo.longVersionCode}, VersionName: ${packageInfo.versionName}")
                } catch (e: Exception) {
                    Log.d("UpdatesFragment", "   ⚠️ NÃO DETECTADO NO DISPOSITIVO - MAS MANTENDO REGISTRO")
                    // CORREÇÃO: Não remover automaticamente - pode ser um problema de detecção
                }
            }
        }
    }

    private fun checkIndividualAppUpdate(packageName: String, currentVersionCode: Long) {
        Log.d("UpdatesFragment", "   🔍 Verificando atualização manual para: $packageName")
        Log.d("UpdatesFragment", "      VersionCode atual: $currentVersionCode")
        // Esta é uma verificação manual simples
    }

    private fun checkForUpdates() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            Log.d("UpdatesFragment", "🔄 Iniciando busca por atualizações...")
            val result = UpdatesFetcher.checkForUpdates(requireContext())
            binding.progressBar.visibility = View.GONE

            result.onSuccess { apps ->
                Log.d("UpdatesFragment", "✅ Busca concluída. ${apps.size} atualizações encontradas")
                if (apps.isEmpty()) {
                    Toast.makeText(requireContext(), "Todos os apps estão atualizados.", Toast.LENGTH_SHORT).show()
                    Log.d("UpdatesFragment", "ℹ️ Nenhuma atualização disponível no momento")

                    // DEBUG: Mostrar por que não encontrou atualizações
                    debugWhyNoUpdates()
                } else {
                    Log.d("UpdatesFragment", "🎉 Mostrando ${apps.size} atualizações disponíveis!")
                    val adapter = UpdatesAdapter(apps) { app -> openAppDetail(app) }
                    binding.updatesRecyclerView.adapter = adapter
                    binding.updatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    Toast.makeText(requireContext(), "${apps.size} atualização(ões) disponível(eis)", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                Log.e("UpdatesFragment", "💥 Erro ao verificar atualizações", error)
                Toast.makeText(requireContext(), "Erro ao verificar atualizações: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun debugForceRegistration() {
        val prefs = requireContext().getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Forçar registro do app de teste
        editor.putString("btcemais.notepad", "0.2") // Versão anterior
        editor.apply()

        Log.d("UpdatesFragment", "🧪 REGISTRO FORÇADO DIRETO NO FRAGMENT")
        Log.d("UpdatesFragment", "   btcemais.notepad -> 0.2")

        // Verificar
        val saved = prefs.getString("btcemais.notepad", "NÃO_SALVOU")
        Log.d("UpdatesFragment", "   ✅ Verificação: $saved")

        Toast.makeText(requireContext(), "Registro forçado - verifique Updates", Toast.LENGTH_SHORT).show()
    }

    private fun debugWhyNoUpdates() {
        val prefs = requireContext().getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val installedPackages = prefs.all.keys.toList()

        Log.d("UpdatesFragment", "=== DEBUG: POR QUE NÃO HÁ ATUALIZAÇÕES? ===")
        Log.d("UpdatesFragment", "Apps registrados: $installedPackages")

        if (installedPackages.isEmpty()) {
            Log.d("UpdatesFragment", "❌ RAZÃO: Nenhum app registrado no SharedPreferences")
            return
        }

        installedPackages.forEach { packageName ->
            Log.d("UpdatesFragment", "🔍 Analisando: $packageName")
            try {
                val packageInfo = requireContext().packageManager.getPackageInfo(packageName, 0)
                Log.d("UpdatesFragment", "   📱 Versão instalada: ${packageInfo.longVersionCode}")

                // Aqui você poderia verificar manualmente contra o JSON
                // mas a lógica completa está no UpdatesFetcher
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("UpdatesFragment", "   ❌ App não encontrado (deveria ter sido removido)")
            }
        }
    }

    private fun openAppDetail(app: InstalledApp) {
        Log.d("UpdatesFragment", "📱 Abrindo detalhes do app: ${app.name}")
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

    override fun onResume() {
        super.onResume()
        Log.d("UpdatesFragment", "🔄 UpdatesFragment onResume - recarregando...")
        debugInstalledApps()
        checkForUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}