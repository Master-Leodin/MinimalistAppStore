// App.kt
package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class App(
    val name: String,
    val developer: String,
    val description: String,
    val iconUrl: String,
    val apkUrl: String, // URL para download do APK
    val version: String,
    val openSourceUrl: String // URL para o código-fonte (GitHub)
) : Parcelable