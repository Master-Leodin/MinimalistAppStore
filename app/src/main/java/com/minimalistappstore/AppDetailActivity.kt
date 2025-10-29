package com.minimalistappstore

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
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
    private var apkUri: Uri? = null

    // Implementação da Activity Result API (Substitui onActivityResult)
    private val requestInstallPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        // Verifica a permissão após o retorno do usuário
        checkPermissionAndStartDownload()
    }

    override fun onResume() {
        super.onResume()
        // Garante que o estado do botão seja atualizado quando a Activity volta ao foco
        setupUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentApp = intent.getParcelableExtra("APP_EXTRA")!!

        // 1. Configura a Toolbar
        setSupportActionBar(binding.toolbar)

        // 2. Configura a ActionBar para exibir o botão de voltar
        supportActionBar?.apply {
            title = currentApp.name
            setDisplayHomeAsUpEnabled(true)
        }
    }

    // Trata o clique no botão de voltar (Up button)
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupUI() {
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name
        binding.detailVersionTextView.text = getString(R.string.version_label, currentApp.version)
        binding.detailDescriptionTextView.text = currentApp.description

        // Lógica para verificar o estado de instalação e configurar o botão
        if (isAppInstalledByStore(currentApp)) {
            binding.downloadButton.text = getString(R.string.uninstall_button)
            binding.downloadButton.setOnClickListener {
                uninstallApp()
                // Após iniciar a desinstalação, removemos o registro local e atualizamos a UI
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
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_dialog_title))
            .setMessage(getString(R.string.install_dialog_message))
            .setPositiveButton(getString(R.string.install_dialog_positive_button)) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse(String.format("package:%s", packageName))
                }
                // Usa a nova Activity Result API
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
            // Chamada para salvar o registro de instalação (agora salva a versão)
            saveInstalledApp(currentApp)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível iniciar a instalação.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_dialog_title))
            .setMessage(getString(R.string.install_dialog_message))
            .setPositiveButton(getString(R.string.install_dialog_positive_button)) { _, _ ->
                showFinalInstallStep()
            }
            .setNegativeButton(getString(R.string.install_dialog_negative_button), null)
            .show()
    }

    private fun showFinalInstallStep() {
        val message = getString(R.string.final_dialog_message, currentApp.name)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.final_dialog_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.final_dialog_ok_button), null)
            .show()
    }

    private fun saveInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Salva a versão que a loja instalou, usando o nome do app como chave
        editor.putString(app.name, app.version)
        editor.apply()
    }

    private fun removeInstalledApp(app: App) {
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(app.name)
        editor.apply()
    }

    // Verifica se o app está instalado no sistema E se foi instalado por esta loja (versão compatível)
    private fun isAppInstalledByStore(app: App): Boolean {
        // 1. Verifica se o app está instalado no sistema
        val isInstalledOnDevice = try {
            packageManager.getPackageInfo(app.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalledOnDevice) return false

        // 2. Verifica se a loja registrou a instalação
        val prefs: SharedPreferences = getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val registeredVersion = prefs.getString(app.name, null)

        // Se a versão registrada for igual à versão atual do app na loja, consideramos que foi instalado pela loja
        return registeredVersion == app.version
    }

    private fun uninstallApp() {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            // O nome do pacote real do app deve ser usado aqui.
            // Assumindo que currentApp.packageName contém o nome do pacote (ex: com.example.app)
            data = Uri.fromParts("package", currentApp.packageName, null)
        }
        startActivity(intent)
    }
}
