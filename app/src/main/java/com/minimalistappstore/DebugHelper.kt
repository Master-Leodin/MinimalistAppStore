package com.minimalistappstore

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

object DebugHelper {

    fun debugEverything(context: Context) {
        Log.d("DebugHelper", "=== INÍCIO DEBUG COMPLETO ===")

        // 1. Debug SharedPreferences
        debugSharedPreferences(context)

        // 2. Debug Apps Instalados
        debugInstalledApps(context)

        // 3. Debug JSONs
        debugJSONs()

        Log.d("DebugHelper", "=== FIM DEBUG COMPLETO ===")
    }

    private fun debugSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
        val allEntries = prefs.all

        Log.d("DebugHelper", "=== SHARED PREFERENCES ===")
        if (allEntries.isEmpty()) {
            Log.d("DebugHelper", "NENHUM APP REGISTRADO NO SHARED PREFERENCES!")
        } else {
            for ((key, value) in allEntries) {
                Log.d("DebugHelper", "📱 App: $key")
                Log.d("DebugHelper", "   Versão registrada: $value")

                // Verifica se o app está realmente instalado
                try {
                    val packageInfo = context.packageManager.getPackageInfo(key, 0)
                    Log.d("DebugHelper", "   ✅ INSTALADO - VersionCode: ${packageInfo.longVersionCode}, VersionName: ${packageInfo.versionName}")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d("DebugHelper", "   ❌ NÃO INSTALADO - Remover do registro")
                    // Remove automaticamente
                    prefs.edit().remove(key).apply()
                }
            }
        }
    }

    private fun debugInstalledApps(context: Context) {
        Log.d("DebugHelper", "=== APPS INSTALADOS NO DISPOSITIVO ===")
        val packages = context.packageManager.getInstalledPackages(0)

        packages.forEach { packageInfo ->
            // Filtra apenas apps de usuário (não sistema)
            if (packageInfo.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) {
                Log.d("DebugHelper", "📦 ${packageInfo.packageName}")
                Log.d("DebugHelper", "   VersionCode: ${packageInfo.longVersionCode}")
                Log.d("DebugHelper", "   VersionName: ${packageInfo.versionName}")
            }
        }
    }

    private fun debugJSONs() {
        Log.d("DebugHelper", "=== URLs DOS JSONs ===")
        Log.d("DebugHelper", "Apps URL: https://pagebroke.netlify.app/json/apps.json")
        Log.d("DebugHelper", "Versions URL: https://pagebroke.netlify.app/json/all_apps_versions.json")
    }
}