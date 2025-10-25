// Website.kt
package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Website(
    val name: String,
    val description: String,
    val url: String
) : Parcelable