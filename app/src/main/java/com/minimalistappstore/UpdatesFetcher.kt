// UpdatesFetcher.kt - VERSÃO CORRIGIDA (com Int)
package com.minimalistappstore

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    val currentVersionCode: Int = 0,
    val latestVersionCode: Int = 0
)

object UpdatesFetcher {
    private const val APPS_URL = "https://pagebroke.netlify.app/json/apps.json"
    private const val VERSIONS_URL = "https://pagebroke.netlify.app/json/all_apps_versions.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(context: Context): Result<List<InstalledApp>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UpdatesFetcher", "🚀 INICIANDO VERIFICAÇÃO DE ATUALIZAÇÕES")

                val prefs = context.getSharedPreferences("installed_apps", Context.MODE_PRIVATE)
                val installedPackages = prefs.all.keys.toList()

                Log.d("UpdatesFetcher", "📋 Apps no SharedPreferences: $installedPackages")

                if (installedPackages.isEmpty()) {
                    Log.d("UpdatesFetcher", "❌ NENHUM APP REGISTRADO")
                    return@withContext Result.success(emptyList())
                }

                // Busca a lista de todos os apps disponíveis
                Log.d("UpdatesFetcher", "🌐 Baixando apps.json...")
                val allAppsJsonString = URL(APPS_URL).readText()
                val allApps: Map<String, App> = json.decodeFromString<List<App>>(allAppsJsonString)
                    .associateBy { it.packageName }

                Log.d("UpdatesFetcher", "✅ Apps disponíveis na loja: ${allApps.keys}")

                // Busca as versões mais recentes
                Log.d("UpdatesFetcher", "🌐 Baixando all_apps_versions.json...")
                val allVersionsJsonString = URL(VERSIONS_URL).readText()
                val allVersionsMap: Map<String, Map<String, Any>> = json.decodeFromString(allVersionsJsonString)
                Log.d("UpdatesFetcher", "🗺️ Mapa de versões decodificado. Chaves: ${allVersionsMap.keys}")

                val pm = context.packageManager
                val appsWithUpdates = mutableListOf<InstalledApp>()

                for (packageName in installedPackages) {
                    try {
                        Log.d("UpdatesFetcher", "🔍 Verificando: $packageName")

                        // Verifica se o app está realmente instalado
                        var currentVersionCode: Int = 0
                        var isInstalled = false

                        try {
                            val packageInfo = pm.getPackageInfo(packageName, 0)
                            currentVersionCode = packageInfo.longVersionCode.toInt() // Convertendo para Int
                            isInstalled = true
                            Log.d("UpdatesFetcher", "   📱 Versão INSTALADA - VersionCode: $currentVersionCode")
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.d("UpdatesFetcher", "   ❌ App não está instalado - removendo do registro")
                           // prefs.edit().remove(packageName).apply()
                            continue // Pula para o próximo app
                        }

                        val appDetails = allApps[packageName]
                        if (appDetails != null) {
                            Log.d("UpdatesFetcher", "   ✅ App encontrado na loja: ${appDetails.name}")

                            // Busca a versão mais recente de forma mais robusta
                            var latestVersionInfo: Map<String, Any>? = null
                            var foundKey: String? = null

                            // Estratégia 1: Busca direta pelo nome do app
                            latestVersionInfo = allVersionsMap[appDetails.name]
                            foundKey = appDetails.name

                            // Estratégia 2: Busca iterativa pelo packageName
                            if (latestVersionInfo == null) {
                                for ((key, value) in allVersionsMap) {
                                    val entryPackageName = value["packageName"] as? String
                                    if (entryPackageName == packageName) {
                                        latestVersionInfo = value
                                        foundKey = key
                                        break
                                    }
                                }
                            }

                            if (latestVersionInfo != null) {
                                Log.d("UpdatesFetcher", "   ✅ Versão encontrada no JSON (chave: $foundKey)")

                                val latestVersionCode = (latestVersionInfo["latestVersionCode"] as? Number)?.toInt() ?: 0
                                val latestVersionName = latestVersionInfo["version"] as? String ?: ""

                                Log.d("UpdatesFetcher", "   📊 COMPARAÇÃO:")
                                Log.d("UpdatesFetcher", "      Instalado: $currentVersionCode")
                                Log.d("UpdatesFetcher", "      Disponível: $latestVersionCode")

                                // Lógica de comparação
                                val needsUpdate = latestVersionCode > currentVersionCode

                                Log.d("UpdatesFetcher", "      Precisa atualizar? $needsUpdate")

                                if (needsUpdate) {
                                    Log.d("UpdatesFetcher", "   🎯 ATUALIZAÇÃO DISPONÍVEL!")

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
                                            currentVersionCode = currentVersionCode,
                                            latestVersionCode = latestVersionCode
                                        )
                                    )
                                }
                            } else {
                                Log.d("UpdatesFetcher", "   ❌ Nenhuma informação de versão encontrada para $packageName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("UpdatesFetcher", "💥 Erro ao verificar app $packageName", e)
                    }
                }

                Log.d("UpdatesFetcher", "📊 RESULTADO FINAL: ${appsWithUpdates.size} atualizações encontradas")
                Result.success(appsWithUpdates)
            } catch (e: Exception) {
                Log.e("UpdatesFetcher", "💥 ERRO GERAL ao verificar atualizações", e)
                Result.failure(e)
            }
        }
    }
}