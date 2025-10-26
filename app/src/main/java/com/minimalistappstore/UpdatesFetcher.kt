package com.minimalistappstore

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

data class InstalledApp(
    val name: String,
    val developer: String,
    val description: String,
    val iconUrl: String,
    val apkUrl: String,
    val version: String,
    val latestVersionName: String,
    val openSourceUrl: String,
    val packageName: String,
    val currentVersionCode: Long,
    val latestVersionCode: Long
)

object UpdatesFetcher {
    private const val APPS_URL = "https://SEU-SITIO.netlify.app/apps.json"
    private const val VERSIONS_URL = "https://SEU-SITIO.netlify.app/all_apps_versions.json"
    private val json = Json { ignoreUnknownKeys = true }

    // Função de extensão para ler o texto de uma URL de forma segura
    private fun URL.readText(): String {
        return openConnection().getInputStream().bufferedReader().use { it.readText() }
    }

    suspend fun checkForUpdates(context: Context): Result<List<InstalledApp>> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
                val installedPackages = prefs.all.keys.toList()

                if (installedPackages.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // CORREÇÃO: Usa a função de extensão segura e corrigida
                val allAppsJsonString = URL(APPS_URL).readText()
                val allApps: Map<String, App> = json.decodeFromString<List<App>>(allAppsJsonString).associateBy { it.packageName }

                val allVersionsJsonString = URL(VERSIONS_URL).readText()
                val allVersions: Map<String, Map<String, Any>> = json.decodeFromString(allVersionsJsonString)

                val pm = context.packageManager
                val appsWithUpdates = mutableListOf<InstalledApp>()

                for (packageName in installedPackages) {
                    try {
                        val packageInfo = pm.getPackageInfo(packageName, 0)
                        val currentVersion = packageInfo.longVersionCode

                        val appDetails = allApps[packageName]
                        if (appDetails != null) {
                            val latestVersionInfo = allVersions[packageName]
                            val latestVersion = (latestVersionInfo?.get("latestVersionCode") as? Double)?.toLong() ?: 0L
                            val latestVersionName = (latestVersionInfo?.get("latestVersionName") as? String) ?: ""

                            if (latestVersion > currentVersion) {
                                appsWithUpdates.add(
                                    InstalledApp(
                                        name = appDetails.name,
                                        developer = appDetails.developer,
                                        description = appDetails.description,
                                        iconUrl = appDetails.iconUrl,
                                        apkUrl = appDetails.apkUrl,
                                        version = appDetails.version,
                                        latestVersionName = latestVersionName,
                                        openSourceUrl = appDetails.openSourceUrl,
                                        packageName = packageName,
                                        currentVersionCode = currentVersion,
                                        latestVersionCode = latestVersion
                                    )
                                )
                            }
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w("UpdatesFetcher", "App $packageName não encontrado mais no dispositivo.")
                    }
                }
                Result.success(appsWithUpdates)
            } catch (e: Exception) {
                Log.e("UpdatesFetcher", "Erro ao verificar atualizações.", e)
                Result.failure(e)
            }
        }
    }
}