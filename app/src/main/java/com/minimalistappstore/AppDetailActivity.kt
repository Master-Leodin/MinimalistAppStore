package com.minimalistappstore

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var apkUri: Uri? = null

    private val requestInstallPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionAndStartDownload()
    }

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentApp = intent.getParcelableExtra("APP_EXTRA")!!

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = currentApp.name
            setDisplayHomeAsUpEnabled(true)
        }

        setupUI()
        setupScreenshots() // Esta linha estava faltando!
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

        if (isAppInstalledByStore(currentApp)) {
            binding.downloadButton.text = getString(R.string.uninstall_button)
            binding.downloadButton.setOnClickListener {
                uninstallApp()
                removeInstalledApp(currentApp)
                setupUI()
            }
        } else {
            binding.downloadButton.text = getString(R.string.download_button)
            binding.downloadButton.setOnClickListener {
                checkPermissionAndStartDownload()
            }
        }
    }

    // ADICIONE ESTA FUNÇÃO QUE ESTAVA FALTANDO:
    private fun setupScreenshots() {
        val screenshotUrls = currentApp.screenshotUrls

        if (screenshotUrls.isNotEmpty()) {
            binding.screenshotsTitleTextView.visibility = android.view.View.VISIBLE
            binding.screenshotsRecyclerView.visibility = android.view.View.VISIBLE

            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.screenshotsRecyclerView.layoutManager = layoutManager

            val adapter = ScreenshotAdapter(screenshotUrls) { imageUrl, position ->
                showFullscreenImage(imageUrl, screenshotUrls, position)
            }
            binding.screenshotsRecyclerView.adapter = adapter
        } else {
            binding.screenshotsTitleTextView.visibility = android.view.View.GONE
            binding.screenshotsRecyclerView.visibility = android.view.View.GONE
        }
    }

    // E ESTA FUNÇÃO TAMBÉM:
    private fun showFullscreenImage(imageUrl: String, allImageUrls: List<String>, startPosition: Int) {
        // Vamos usar a versão SIMPLES primeiro (sem ViewPager)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image) // Use o layout simples

        val imageView = dialog.findViewById<android.widget.ImageView>(R.id.fullscreenImageView)
        val progressBar = dialog.findViewById<android.widget.ProgressBar>(R.id.fullscreenProgressBar)
        val closeButton = dialog.findViewById<android.widget.ImageButton>(R.id.closeButton)

        // Carrega a imagem
        progressBar.visibility = android.view.View.VISIBLE
        imageView.load(imageUrl) {
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    progressBar.visibility = android.view.View.GONE
                },
                onError = { _, _ ->
                    progressBar.visibility = android.view.View.GONE
                    imageView.setImageResource(R.drawable.screenshot_placeholder)
                }
            )
        }

        // Fecha o dialog ao clicar no botão X
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Fecha o dialog ao clicar na imagem
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        // Fecha o dialog ao pressionar back
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        dialog.show()
    }

    // ... (o resto do seu código existente permanece igual)
    private fun checkPermissionAndStartDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageManager = packageManager
            val canInstallPackages = packageManager.canRequestPackageInstalls()

            if (!canInstallPackages) {
                showRequestPermissionDialog()
            } else {
                startDownload()
            }
        } else {
            startDownload()
        }
    }

    private fun showRequestPermissionDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_dialog_title))
            .setMessage(getString(R.string.install_dialog_message))
            .setPositiveButton(getString(R.string.install_dialog_positive_button)) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse(String.format("package:%s", packageName))
                }
                requestInstallPermissionLauncher.launch(intent)
            }
            .setNegativeButton(getString(R.string.install_dialog_negative_button), null)
            .show()
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
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(installIntent)
            saveInstalledApp(currentApp)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível iniciar a instalação.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(app.name, app.version)
        editor.apply()
    }

    private fun removeInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(app.name)
        editor.apply()
    }

    private fun isAppInstalledByStore(app: App): Boolean {
        val isInstalledOnDevice = try {
            packageManager.getPackageInfo(app.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalledOnDevice) return false

        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val registeredVersion = prefs.getString(app.name, null)

        return registeredVersion == app.version
    }

    private fun uninstallApp() {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", currentApp.packageName, null)
        }
        startActivity(intent)
    }
}