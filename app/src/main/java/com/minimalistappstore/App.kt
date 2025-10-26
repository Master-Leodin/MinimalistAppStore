package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class App(
    var name: String = "",
    var developer: String = "",
    var description: String = "",
    var iconUrl: String = "",
    var apkUrl: String = "",
    var version: String = "",
    var openSourceUrl: String = "",
    var packageName: String = "" // ESTA LINHA É ESSENCIAL
) : Parcelable