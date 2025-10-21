// AppDetailActivity.kt
package com.minimalistappstore

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.minimalistappstore.databinding.ActivityAppDetailBinding

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var currentApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recupera o objeto App passado da MainActivity
        currentApp = intent.getParcelableExtra("APP_EXTRA")!!

        setupUI()
    }

    private fun setupUI() {
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name
        binding.detailVersionTextView.text = "Versão ${currentApp.version}"
        binding.detailDescriptionTextView.text = currentApp.description

        binding.downloadButton.setOnClickListener {
            simulateDownloadAndInstall()
        }
    }

    private fun simulateDownloadAndInstall() {
        // 1. Muda o estado do botão para "Baixando..."
        binding.downloadButton.text = "Baixando..."
        binding.downloadButton.isEnabled = false

        // 2. Simula um delay de 3 segundos para o "download"
        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Muda o estado para "Instalar"
            binding.downloadButton.text = "Instalar"
            binding.downloadButton.isEnabled = true

            // 4. Configura a ação de clique para o botão "Instalar"
            binding.downloadButton.setOnClickListener {
                showInstallDialog()
            }
        }, 3000) // 3000ms = 3s
    }

    private fun showInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Instalação Manual Necessária")
            .setMessage("Para sua segurança, apps de fora da Play Store exigem uma etapa extra.\n\n" +
                    "1. Após clicar em 'OK', você será redirecionado para as Configurações.\n" +
                    "2. Ative a permissão para \"Permitir instalação de apps desconhecidos\" para esta fonte.\n" +
                    "3. Volte para o app e toque em 'Instalar' novamente.")
            .setPositiveButton("OK, entendi") { _, _ ->
                // Aqui você abriria as Configurações do Android para o usuário.
                // Em um app real, você usaria um Intent para abrir a tela de instalação de apps.
                // Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", packageName)))
                showFinalInstallStep()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFinalInstallStep() {
        // Simula o clique final que acionaria o Intent de instalação do APK
        AlertDialog.Builder(this)
            .setTitle("Pronto para Instalar!")
            .setMessage("Agora o app ${currentApp.name} seria instalado. Em um cenário real, o arquivo APK baixado seria aberto pelo sistema Android.")
            .setPositiveButton("Entendido", null)
            .show()
    }
}