package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.minimalistappstore.databinding.ActivityMainBinding
import com.minimalistappstore.databinding.DialogUpdateAvailableBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var updateApkFile: File? = null
    private var updateDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        checkForAppUpdate()
        setupToolbar()

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_apps
        }

        fun setupToolbar() {
            setSupportActionBar(binding.toolbar)

            // SOLUÇÃO SIMPLES - APENAS DEFINIR O ÍCONE E A COR
            binding.toolbar.setNavigationIcon(R.drawable.ic_menu) // Use seu ícone de menu aqui
            binding.toolbar.navigationIcon?.setTint(getColor(R.color.white)) // Ou a cor que quiser

            binding.toolbar.setNavigationOnClickListener {
                if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                    binding.drawerLayout.closeDrawer(binding.navigationView)
                } else {
                    binding.drawerLayout.openDrawer(binding.navigationView)
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: run {
                error("Could not find NavHostFragment with id R.id.nav_host_fragment")
            }

        val navController = navHostFragment.navController

        // Configuração para as 4 abas: APPs, Jogos, Websites, Doações
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_apps, R.id.nav_games, R.id.nav_websites, R.id.nav_donate),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.let { it as? NavHostFragment }?.navController
        return navController?.navigateUp(appBarConfiguration) ?: super.onSupportNavigateUp()
    }

    // MANTER: Lógica de checagem de atualização da própria loja
    private fun checkForAppUpdate() {
        lifecycleScope.launch {
            val updateInfo = VersionChecker.checkForUpdate(this@MainActivity)
            updateInfo?.let {
                showUpdateDialog(it)
            }
        }
    }

    private fun showUpdateDialog(version: AppVersion) {
        val dialogBinding = DialogUpdateAvailableBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false) // Impede que o usuário feche clicando fora
            .create()

        updateDialog = dialog // Guarda a referência do diálogo

        dialogBinding.releaseNotesTextView.text = version.releaseNotes

        // Configura o botão de atualização para download interno
        dialogBinding.updateButton.setOnClickListener {
            startUpdateDownload(version.apkUrl, dialogBinding)
        }

        // Configura o botão "Mais tarde"
        dialogBinding.laterButton.setOnClickListener {
            dialog.dismiss()
            updateDialog = null
        }

        dialog.show()
    }

    private fun startUpdateDownload(apkUrl: String, dialogBinding: DialogUpdateAvailableBinding) {
        // Desabilita os botões e mostra o loading
        dialogBinding.updateButton.isEnabled = false
        dialogBinding.laterButton.isEnabled = false
        dialogBinding.updateButton.text = "Baixando..."
        dialogBinding.updateProgressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection()
                connection.connect()

                val contentLength = connection.contentLength
                val inputStream = connection.getInputStream()

                // Cria arquivo temporário no cache
                val apkFile = File(cacheDir, "MinimalistAppStore_update.apk")
                val outputStream = FileOutputStream(apkFile)

                var totalBytesRead = 0
                val buffer = ByteArray(8 * 1024) // 8KB buffer
                var bytesRead: Int

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Atualiza o progresso (opcional)
                            if (contentLength > 0) {
                                withContext(Dispatchers.Main) {
                                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                                    dialogBinding.updateProgressBar.progress = progress
                                }
                            }
                        }
                    }
                }

                updateApkFile = apkFile

                withContext(Dispatchers.Main) {
                    // Esconde a barra de progresso e restaura o botão
                    dialogBinding.updateProgressBar.visibility = android.view.View.GONE
                    dialogBinding.updateButton.text = "Instalar"
                    dialogBinding.updateButton.isEnabled = true
                    dialogBinding.laterButton.isEnabled = true

                    triggerUpdateInstallation(apkFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Restaura a UI em caso de erro
                    dialogBinding.updateProgressBar.visibility = android.view.View.GONE
                    dialogBinding.updateButton.text = "Tentar Novamente"
                    dialogBinding.updateButton.isEnabled = true
                    dialogBinding.laterButton.isEnabled = true

                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Erro no download da atualização: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun triggerUpdateInstallation(apkFile: File) {
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
            // Fecha o diálogo após iniciar a instalação
            updateDialog?.dismiss()
            updateDialog = null
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Não foi possível iniciar a instalação da atualização.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Limpar arquivo temporário quando necessário
    override fun onDestroy() {
        updateApkFile?.delete()
        updateDialog?.dismiss()
        super.onDestroy()
    }
}
