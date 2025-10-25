// WebsiteFetcher.kt
package com.minimalistappstore

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

object WebsiteFetcher {
    // URL do arquivo JSON para os sites
    private const val WEBSITES_JSON_URL = "https://raw.githubusercontent.com/Master-Leodin/MinimalistAppStore/refs/heads/main/websites.json"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchWebsites(): Result<List<Website>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WebsiteFetcher", "Tentando baixar JSON de sites da URL: $WEBSITES_JSON_URL")
                val url = URL(WEBSITES_JSON_URL)
                val jsonString = url.readText()
                Log.d("WebsiteFetcher", "JSON de sites baixado com sucesso.")
                val websites = json.decodeFromString<List<Website>>(jsonString)
                Result.success(websites)
            } catch (e: Exception) {
                Log.e("WebsiteFetcher", "Erro ao buscar ou processar o JSON de sites.", e)
                Result.failure(e)
            }
        }
    }
}