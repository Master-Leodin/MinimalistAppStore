package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class App(
    val name: String = "",
    val developer: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val apkUrl: String = "",
    val version: String = "",
    val latestVersionCode: Int = 0,
    val openSourceUrl: String = "",
    val packageName: String = "",
    val screenshotUrls: List<String> = emptyList()
) : Parcelable
