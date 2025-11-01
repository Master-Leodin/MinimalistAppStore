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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        checkForAppUpdate()

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_apps
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
            .setCancelable(false)
            .create()

        dialogBinding.releaseNotesTextView.text = version.releaseNotes

        // Configura o botão de atualização para download interno
        dialogBinding.updateButton.setOnClickListener {
            dialog.dismiss()
            startUpdateDownload(version.apkUrl)
        }

        // Configura o botão "Mais tarde"
        dialogBinding.laterButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startUpdateDownload(apkUrl: String) {
        // Mostrar progresso (opcional - você pode adicionar uma barra de progresso no diálogo)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()

                // Cria arquivo temporário no cache
                val apkFile = File(cacheDir, "MinimalistAppStore_update.apk")
                val outputStream = FileOutputStream(apkFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                updateApkFile = apkFile

                withContext(Dispatchers.Main) {
                    triggerUpdateInstallation(apkFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Mostrar erro para o usuário
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
        super.onDestroy()
    }
}