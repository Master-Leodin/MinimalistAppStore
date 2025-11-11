package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.minimalistappstore.databinding.ActivityAppDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var currentApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // O getParcelableExtra foi depreciado, mas mantendo a compatibilidade com o código original
        @Suppress("DEPRECATION")
        currentApp = intent.getParcelableExtra("APP_EXTRA")!!

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = currentApp.name
            setDisplayHomeAsUpEnabled(true)
        }

        setupUI()
        setupScreenshots()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupUI() {
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name
        binding.detailVersionTextView.text = getString(R.string.version_label, currentApp.version)
        binding.detailDescriptionTextView.text = currentApp.description

        // Simplificando a lógica de botão: sempre mostra "Baixar"
        binding.downloadButton.text = getString(R.string.download_button)
        binding.downloadButton.setOnClickListener {
            startDownload()
        }
    }

    private fun setupScreenshots() {
        // Manter a lógica de screenshots
    }

    private fun startDownload() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.downloadButton.isEnabled = false
        binding.downloadButton.text = getString(R.string.downloading_button)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apkUrl = URL(currentApp.apkUrl)
                val connection = apkUrl.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()

                val apkFile = File(cacheDir, "${currentApp.name}_v${currentApp.version}.apk")
                val outputStream = FileOutputStream(apkFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = getString(R.string.install_button)
                    triggerInstallation(apkFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = getString(R.string.download_button)
                    Toast.makeText(this@AppDetailActivity, "Erro no download: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerInstallation(apkFile: File) {
        val apkUri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível iniciar a instalação.", Toast.LENGTH_SHORT).show()
        }
    }
}
