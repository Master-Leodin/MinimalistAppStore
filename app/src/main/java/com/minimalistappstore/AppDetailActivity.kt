package com.minimalistappstore

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
        Log.d("AppDetailActivity", "=== STATUS APP ATUAL ===")
        Log.d("AppDetailActivity", "Package: ${currentApp.packageName}")
        Log.d("AppDetailActivity", "Instalado: ${isAppInstalled(currentApp.packageName)}")
        Log.d("AppDetailActivity", "Registrado: ${isAppRegistered(currentApp.packageName)}")
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isAppRegistered(packageName: String): Boolean {
        val prefs = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        return prefs.getString(packageName, null) != null
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
        setupScreenshots()

        // DEBUG: Botão temporário para forçar registro
        binding.downloadButton.setOnLongClickListener {
            Log.d("AppDetailActivity", "🔄 FORÇANDO REGISTRO MANUAL...")
            forceRegisterForTesting()
            Toast.makeText(this, "Registro forçado para teste", Toast.LENGTH_SHORT).show()
            true
        }

        debugAppInfo()
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

    private fun showFullscreenImage(imageUrl: String, allImageUrls: List<String>, startPosition: Int) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val imageView = dialog.findViewById<android.widget.ImageView>(R.id.fullscreenImageView)
        val progressBar = dialog.findViewById<android.widget.ProgressBar>(R.id.fullscreenProgressBar)
        val closeButton = dialog.findViewById<android.widget.ImageButton>(R.id.closeButton)

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

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        imageView.setOnClickListener {
            dialog.dismiss()
        }

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
            // CORREÇÃO: Salvar ANTES de iniciar a instalação
            saveInstalledApp(currentApp)
            Log.d("AppDetailActivity", "✅ App registrado ANTES da instalação: ${currentApp.packageName}")

            startActivity(installIntent)
            Log.d("AppDetailActivity", "🚀 Instalação iniciada para: ${currentApp.packageName}")
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível iniciar a instalação.", Toast.LENGTH_SHORT).show()
            Log.e("AppDetailActivity", "❌ Erro ao iniciar instalação", e)
        }
    }

    private fun saveInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // CORREÇÃO: Usar apply() em vez de commit() e adicionar verificação
        editor.putString(app.packageName, app.version)

        // CORREÇÃO CRÍTICA: Garantir que o registro foi salvo
        if (editor.commit()) { // Usar commit() para garantir sincronização
            Log.d("AppDetailActivity", "✅ APP SALVO COM SUCESSO: ${app.packageName} -> ${app.version}")

            // Verificação imediata
            val savedVersion = prefs.getString(app.packageName, "NÃO_SALVOU")
            Log.d("AppDetailActivity", "🔍 CONFIRMAÇÃO: $savedVersion")

            // Debug: listar todos os apps registrados
            val allEntries = prefs.all
            Log.d("AppDetailActivity", "=== TODOS OS APPS REGISTRADOS ===")
            allEntries.forEach { (key, value) ->
                Log.d("AppDetailActivity", "📱 $key -> $value")
            }
        } else {
            Log.e("AppDetailActivity", "❌ FALHA AO SALVAR APP: ${app.packageName}")
        }
    }

    // CORREÇÃO: Método melhorado para forçar registro
    private fun forceRegisterForTesting() {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Registrar com versão anterior para simular necessidade de update
        editor.putString(currentApp.packageName, "0.2") // Versão ANTERIOR
        editor.apply()

        Log.d("AppDetailActivity", "🧪 REGISTRO FORÇADO PARA TESTE")
        Log.d("AppDetailActivity", "   Package: ${currentApp.packageName}")
        Log.d("AppDetailActivity", "   Versão registrada: 0.2 (anterior)")
        Log.d("AppDetailActivity", "   Versão disponível: ${currentApp.version} (nova)")

        // Verificação imediata
        val saved = prefs.getString(currentApp.packageName, "NÃO_SALVOU")
        Log.d("AppDetailActivity", "   ✅ Verificação: $saved")

        // Atualizar UI
        setupUI()

        Toast.makeText(this, "App registrado para teste de atualização!", Toast.LENGTH_LONG).show()
    }

    private fun removeInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(app.packageName)
        editor.apply()
        Log.d("AppDetailActivity", "🗑️ APP REMOVIDO DO REGISTRO: ${app.packageName}")
    }

    private fun isAppInstalledByStore(app: App): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(app.packageName, 0)
            val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
            val registeredVersion = prefs.getString(app.packageName, null)

            // Considerar instalado se estiver no registro OU se estiver instalado no dispositivo
            val isRegistered = registeredVersion != null
            val isInstalled = packageInfo != null

            isInstalled || isRegistered // ← Mudança importante aqui
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun uninstallApp() {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", currentApp.packageName, null)
        }
        startActivity(intent)
    }

    // MÉTODOS DEBUG ADICIONAIS
    private fun debugAppInfo() {
        Log.d("AppDetailActivity", "=== DEBUG APP INFO ===")
        Log.d("AppDetailActivity", "Nome: ${currentApp.name}")
        Log.d("AppDetailActivity", "Package: ${currentApp.packageName}")
        Log.d("AppDetailActivity", "Versão: ${currentApp.version}")

        try {
            val packageInfo = packageManager.getPackageInfo(currentApp.packageName, 0)
            Log.d("AppDetailActivity", "✅ INSTALADO - VersionCode: ${packageInfo.longVersionCode}, VersionName: ${packageInfo.versionName}")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("AppDetailActivity", "❌ NÃO INSTALADO")
        }
    }

    private fun debugAppRegistration() {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val allEntries = prefs.all

        Log.d("AppDetailActivity", "=== DEBUG REGISTRO ONRESUME ===")
        if (allEntries.isEmpty()) {
            Log.d("AppDetailActivity", "NENHUM APP REGISTRADO NO SHAREDPREFERENCES!")
        } else {
            for ((key, value) in allEntries) {
                Log.d("AppDetailActivity", "📱 $key -> $value")
            }
        }

        // Verificar especificamente o app atual
        val currentAppRegistered = prefs.getString(currentApp.packageName, null)
        Log.d("AppDetailActivity", "App atual registrado? ${currentAppRegistered != null}")
        Log.d("AppDetailActivity", "Valor registrado: $currentAppRegistered")
    }
}