package com.minimalistappstore

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.minimalistappstore.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_apps
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigation() {
        // Encontra o NavHostFragment de forma segura
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: run {
                // Se for nulo, mostra um erro e para a execução para evitar o crash
                error("Could not find NavHostFragment with id R.id.nav_host_fragment")
            }

        val navController = navHostFragment.navController

        // Configura a AppBar
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_apps, R.id.nav_games, R.id.nav_websites, R.id.nav_donate),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Configura o menu gaveta
        binding.navigationView.setupWithNavController(navController)

        // Configura a barra inferior
        binding.bottomNavigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.let { it as? NavHostFragment }?.navController
        return navController?.navigateUp(appBarConfiguration) ?: super.onSupportNavigateUp()
    }
}