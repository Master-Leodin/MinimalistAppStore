// DonationFetcher.kt
package com.minimalistappstore

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

object DonationFetcher {
    private const val DONATIONS_JSON_URL = "https://raw.githubusercontent.com/Master-Leodin/MinimalistAppStore/refs/heads/main/donations.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchDonations(): Result<List<DonationOption>> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL(DONATIONS_JSON_URL).readText()
                val donations = json.decodeFromString<List<DonationOption>>(jsonString)
                Result.success(donations)
            } catch (e: Exception) {
                Log.e("DonationFetcher", "Erro ao buscar ou processar o JSON de doações.", e)
                Result.failure(e)
            }
        }
    }
}