// AppDataSource.kt
package com.minimalistappstore

import android.content.Context

object AppDataSource {

    /**
     * Retorna uma lista de apps, com nomes e descrições traduzidos
     * de acordo com o idioma do sistema.
     * @param context O contexto da Activity para acessar os recursos de string.
     */
    fun getApps(context: Context): List<App> {
        return listOf(
            App(
                name = context.getString(R.string.app_focus_zen_name),
                developer = "Indie Studio",
                description = context.getString(R.string.app_focus_zen_description),
                iconUrl = "https://i.imgur.com/g7p0a1L.png",
                apkUrl = "https://mega.nz/file/YOUR_DIRECT_LINK_ID#download_key",
                version = "1.2.0",
                openSourceUrl = "https://github.com/indiestudio/focus-zen"
            ),
            App(
                name = context.getString(R.string.app_plain_notes_name),
                developer = "Simple Tools Co.",
                description = context.getString(R.string.app_plain_notes_description),
                iconUrl = "https://i.imgur.com/KY2jA9k.png",
                apkUrl = "https://mega.nz/file/ANOTHER_DIRECT_LINK_ID#download_key",
                version = "3.0.1",
                openSourceUrl = "https://github.com/simpletools/plain-notes"
            ),
            App(
                name = context.getString(R.string.app_monochrome_walls_name),
                developer = "Artist Collective",
                description = context.getString(R.string.app_monochrome_walls_description),
                iconUrl = "https://i.imgur.com/uJpN5hW.png",
                apkUrl = "https://mega.nz/file/THIRD_DIRECT_LINK_ID#download_key",
                version = "1.0.5",
                openSourceUrl = "https://github.com/artistcollec/monochrome-walls"
            )
        )
    }
}