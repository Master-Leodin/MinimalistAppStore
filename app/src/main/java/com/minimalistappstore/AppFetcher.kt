package com.minimalistappstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

object AppFetcher {
    private const val APPS_JSON_URL = "https://raw.githubusercontent.com/Master-Leodin/MinimalistAppStore/refs/heads/main/apps.json"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchApps(): Result<List<App>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(APPS_JSON_URL)
                val jsonString = url.readText()
                val apps = json.decodeFromString<List<App>>(jsonString)
                Result.success(apps)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}