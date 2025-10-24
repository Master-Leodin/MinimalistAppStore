// AppDataSource.kt
package com.minimalistappstore

import android.content.Context

object AppDataSource {

    fun getApps(context: Context): List<App> {
        return listOf(
            App(
                name = context.getString(R.string.app_btc_profits_name),
                developer = "Master-Leodin",
                description = context.getString(R.string.app_btc_profits_description),
                // IMPORTANTE: Substitua esta URL pelo link do seu próprio ícone!
                iconUrl = "https://i.imgur.com/gKd5h2S.png",
                // LINK CORRIGIDO
                apkUrl = "https://github.com/Master-Leodin/LucrosBTC/releases/download/BTC_0.1.9/BTC-0-1-9.apk",
                version = "0.1.9",
                openSourceUrl = "https://github.com/Master-Leodin/LucrosBTC"
            )
        )
    }
}