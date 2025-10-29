// VersionChecker.kt
package com.minimalistappstore

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class AppVersion(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val apkUrl: String
)

object VersionChecker {
    // Substitua pela URL do seu arquivo version.json no Netlify
    private const val VERSION_URL = "https://pagebroke.netlify.app/json/version.json"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Obtém a versão atual do app instalado.
     */
    private fun getCurrentVersionCode(context: Context ): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            // Usando longVersionCode.toInt() para compatibilidade com versões mais recentes
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            -1 // Em caso de erro, retorna um valor inválido
        }
    }

    /**
     * Busca a última versão disponível no servidor.
     */
    private suspend fun fetchLatestVersion(): Result<AppVersion> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL(VERSION_URL).readText()
                val version = json.decodeFromString<AppVersion>(jsonString)
                Result.success(version)
            } catch (e: Exception) {
                Log.e("VersionChecker", "Erro ao buscar a versão mais recente.", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica se há uma atualização disponível.
     * Retorna um objeto AppVersion se houver, ou null se não houver ou ocorrer erro.
     */
    suspend fun checkForUpdate(context: Context): AppVersion? {
        val currentVersionCode = getCurrentVersionCode(context)
        if (currentVersionCode == -1) return null // Não foi possível obter a versão atual

        val result = fetchLatestVersion()
        // Retorna a versão mais recente SOMENTE se o código de versão for maior que o atual
        return result.getOrNull()?.takeIf { it.latestVersionCode > currentVersionCode }
    }
}
