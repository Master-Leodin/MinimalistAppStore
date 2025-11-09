// UpdatesFetcher.kt - CORREÇÃO CRÍTICA
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
    val currentVersionCode: Long,
    val latestVersionCode: Long
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

                // CORREÇÃO: Se não há apps registrados, retorna lista vazia
                if (installedPackages.isEmpty()) {
                    Log.d("UpdatesFetcher", "❌ NENHUM APP REGISTRADO - O usuário precisa instalar algum app primeiro")
                    return@withContext Result.success(emptyList())
                }

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
                Log.d("UpdatesFetcher", "📄 Conteúdo do JSON de versões: $allVersionsJsonString")

                val allVersionsMap: Map<String, Map<String, Any>> = json.decodeFromString(allVersionsJsonString)
                Log.d("UpdatesFetcher", "🗺️ Mapa de versões decodificado. Chaves: ${allVersionsMap.keys}")

                val pm = context.packageManager
                val appsWithUpdates = mutableListOf<InstalledApp>()

                for (packageName in installedPackages) {
                    try {
                        Log.d("UpdatesFetcher", "🔍 Verificando: $packageName")

                        // CORREÇÃO: Tentar detectar o app de forma mais robusta
                        var currentVersion: Long = 0
                        var currentVersionName: String = ""
                        var isInstalled = false

                        try {
                            val packageInfo = pm.getPackageInfo(packageName, 0)
                            currentVersion = packageInfo.longVersionCode
                            currentVersionName = packageInfo.versionName ?: ""
                            isInstalled = true
                            Log.d("UpdatesFetcher", "   📱 Versão INSTALADA:")
                            Log.d("UpdatesFetcher", "      VersionCode: $currentVersion")
                            Log.d("UpdatesFetcher", "      VersionName: $currentVersionName")
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.d("UpdatesFetcher", "   ⚠️ App não detectado no dispositivo, mas mantendo no registro")
                            // Continua a verificação mesmo se não detectar o app instalado
                            // Usa a versão do registro como fallback
                            val registeredVersion = prefs.getString(packageName, "")
                            currentVersionName = registeredVersion ?: "0"
                            Log.d("UpdatesFetcher", "   🔄 Usando versão do registro: $currentVersionName")
                        }

                        val appDetails = allApps[packageName]

                        if (appDetails != null) {
                            Log.d("UpdatesFetcher", "   ✅ App encontrado na loja: ${appDetails.name}")

                            // Buscar pela chave correta no all_apps_versions.json
                            var latestVersionInfo: Map<String, Any>? = null
                            var foundKey: String? = null

                            // Primeiro tenta encontrar pelo nome do app
                            for ((key, value) in allVersionsMap) {
                                val entryPackageName = value["packageName"] as? String
                                Log.d("UpdatesFetcher", "   🔎 Procurando em: $key -> packageName: $entryPackageName")
                                if (entryPackageName == packageName) {
                                    latestVersionInfo = value
                                    foundKey = key
                                    break
                                }
                            }

                            // Se não encontrou pelo packageName, tenta pelo nome do app
                            if (latestVersionInfo == null) {
                                latestVersionInfo = allVersionsMap[appDetails.name]
                                foundKey = appDetails.name
                                Log.d("UpdatesFetcher", "   🔄 Tentando buscar pelo nome: ${appDetails.name}")
                            }

                            if (latestVersionInfo != null) {
                                Log.d("UpdatesFetcher", "   ✅ Versão encontrada no JSON (chave: $foundKey)")

                                val latestVersionCode = (latestVersionInfo["latestVersionCode"] as? Number)?.toLong() ?: 0L
                                val latestVersionName = latestVersionInfo["version"] as? String ?: ""

                                Log.d("UpdatesFetcher", "   📦 Versão DISPONÍVEL:")
                                Log.d("UpdatesFetcher", "      LatestVersionCode: $latestVersionCode")
                                Log.d("UpdatesFetcher", "      Version: $latestVersionName")

                                // CORREÇÃO: Se não conseguiu detectar o app instalado, assume que precisa atualizar
                                // ou pelo menos mostra que há uma versão disponível
                                val needsUpdate = if (isInstalled) {
                                    latestVersionCode > currentVersion
                                } else {
                                    // Se não detectou o app, verifica se a versão do registro é diferente da disponível
                                    val registeredVersion = prefs.getString(packageName, "")
                                    latestVersionName != registeredVersion
                                }

                                Log.d("UpdatesFetcher", "   ⚖️ COMPARAÇÃO:")
                                Log.d("UpdatesFetcher", "      Instalado: $currentVersion ($currentVersionName)")
                                Log.d("UpdatesFetcher", "      Disponível: $latestVersionCode ($latestVersionName)")
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
                                            currentVersionCode = currentVersion,
                                            latestVersionCode = latestVersionCode
                                        )
                                    )
                                } else {
                                    Log.d("UpdatesFetcher", "   ✅ App está atualizado")
                                }
                            } else {
                                Log.d("UpdatesFetcher", "   ❌ Nenhuma informação de versão encontrada para $packageName")
                            }
                        } else {
                            Log.d("UpdatesFetcher", "   ❌ App $packageName não encontrado na lista de apps da loja")
                        }
                    } catch (e: Exception) {
                        Log.e("UpdatesFetcher", "💥 Erro ao verificar app $packageName", e)
                    }
                    Log.d("UpdatesFetcher", "---")
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