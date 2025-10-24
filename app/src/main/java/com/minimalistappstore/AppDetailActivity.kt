// AppDetailActivity.kt
package com.minimalistappstore

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
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

        currentApp = intent.getParcelableExtra("APP_EXTRA")!!
        setupUI()
    }

    private fun setupUI() {
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name
        binding.detailVersionTextView.text = getString(R.string.version_label, currentApp.version)
        binding.detailDescriptionTextView.text = currentApp.description
        binding.downloadButton.text = getString(R.string.download_button)

        binding.downloadButton.setOnClickListener {
            checkPermissionAndStartDownload()
        }
    }

    private fun checkPermissionAndStartDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para Android Oreo (API 26) e superior, verifique a permissão
            val packageManager = packageManager
            val canInstallPackages = packageManager.canRequestPackageInstalls()

            if (!canInstallPackages) {
                // Se não tiver permissão, mostre o diálogo para concedê-la
                showRequestPermissionDialog()
            } else {
                // Se já tiver permissão, comece o download
                startDownload()
            }
        } else {
            // Para versões mais antigas, comece o download diretamente
            startDownload()
        }
    }

    private fun showRequestPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_dialog_title))
            .setMessage(getString(R.string.install_dialog_message))
            .setPositiveButton(getString(R.string.install_dialog_positive_button)) { _, _ ->
                // Leva o usuário para a tela de configurações
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse(String.format("package:%s", packageName))
                }
                startActivityForResult(intent, REQUEST_INSTALL_PERMISSION_CODE)
            }
            .setNegativeButton(getString(R.string.install_dialog_negative_button), null)
            .show()
    }

    private fun startDownload() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.downloadButton.isEnabled = false
        binding.downloadButton.text = getString(R.string.downloading_button)

        // Usa uma Coroutine para fazer o download em uma thread de background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apkUrl = URL(currentApp.apkUrl)
                val connection = apkUrl.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()

                // Cria um arquivo temporário no diretório de cache
                val apkFile = File(cacheDir, "${currentApp.name}_v${currentApp.version}.apk")
                val outputStream = FileOutputStream(apkFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Volta para a thread principal para instalar
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = getString(R.string.install_button)
                    triggerInstallation(apkFile)
                }

            } catch (e: Exception) {
                // Em caso de erro no download
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
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Para Android Nougat (API 24) e superior, use FileProvider
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)
        } else {
            // Para versões mais antigas, use o URI do arquivo diretamente
            Uri.fromFile(apkFile)
        }

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

    companion object {
        private const val REQUEST_INSTALL_PERMISSION_CODE = 1001
    }

    // Este método é chamado quando o usuário volta da tela de configurações
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_PERMISSION_CODE) {
            // Verifica novamente a permissão quando o usuário retorna
            checkPermissionAndStartDownload()
        }
    }
}