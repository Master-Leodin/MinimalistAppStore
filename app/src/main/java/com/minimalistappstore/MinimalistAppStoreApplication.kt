// MinimalistAppStoreApplication.kt
package com.minimalistappstore

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MinimalistAppStoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Força o modo claro para o aplicativo inteiro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}