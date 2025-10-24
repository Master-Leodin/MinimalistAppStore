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

    /**
     * Configura a interface do usuário com os dados do aplicativo selecionado.
     */
    private fun setupUI() {
        // Carrega o ícone do app usando a biblioteca Coil
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name

        // Define o texto da versão usando o recurso de string formatada
        binding.detailVersionTextView.text = getString(R.string.version_label, currentApp.version)
        binding.detailDescriptionTextView.text = currentApp.description

        // Define o texto inicial do botão de download
        binding.downloadButton.text = getString(R.string.download_button)

        binding.downloadButton.setOnClickListener {
            simulateDownloadAndInstall()
        }
    }

    /**
     * Simula o processo de download do APK.
     * Em um app real, aqui você faria o download do arquivo em uma thread de background.
     */
    private fun simulateDownloadAndInstall() {
        // 1. Muda o estado do botão para "Baixando..."
        binding.downloadButton.text = getString(R.string.downloading_button)
        binding.downloadButton.isEnabled = false

        // 2. Simula um delay de 3 segundos para o "download"
        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Muda o estado para "Instalar"
            binding.downloadButton.text = getString(R.string.install_button)
            binding.downloadButton.isEnabled = true

            // 4. Configura a ação de clique para o botão "Instalar"
            binding.downloadButton.setOnClickListener {
                showInstallDialog()
            }
        }, 3000) // 3000ms = 3s
    }

    /**
     * Exibe um diálogo instruindo o usuário sobre o processo de instalação manual (sideloading).
     */
    private fun showInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_dialog_title))
            .setMessage(getString(R.string.install_dialog_message))
            .setPositiveButton(getString(R.string.install_dialog_positive_button)) { _, _ ->
                // Aqui você abriria as Configurações do Android para o usuário.
                // Em um app real, você usaria um Intent para abrir a tela de instalação de apps.
                // Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", packageName)))
                showFinalInstallStep()
            }
            .setNegativeButton(getString(R.string.install_dialog_negative_button), null)
            .show()
    }

    /**
     * Exibe o diálogo final que simula a instalação do aplicativo.
     */
    private fun showFinalInstallStep() {
        // Formata a mensagem com o nome do app
        val message = getString(R.string.final_dialog_message, currentApp.name)

        // Simula o clique final que acionaria o Intent de instalação do APK
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.final_dialog_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.final_dialog_ok_button), null)
            .show()
    }
}