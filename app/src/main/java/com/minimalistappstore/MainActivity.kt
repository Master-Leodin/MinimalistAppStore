package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        checkForAppUpdate() // Chamada no onCreate

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
        // Usa o layout de binding para o diálogo
        val dialogBinding = DialogUpdateAvailableBinding.inflate(layoutInflater)

        // Configura o diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false) // Impede que o usuário feche clicando fora
            .create()

        // Exibe as notas de lançamento
        dialogBinding.releaseNotesTextView.text = version.releaseNotes

        // Configura o botão de atualização (Download)
        dialogBinding.updateButton.setOnClickListener {
            // Abre o link direto do APK
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(version.apkUrl))
            startActivity(intent)
            dialog.dismiss()
        }

        // Configura o botão "Mais tarde"
        dialogBinding.laterButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
