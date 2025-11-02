// DonationOption.kt
package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class DonationOption(
    val title: String,
    val subtitle: String,
    val description: String,
    val methods: List<DonationMethod>
) : Parcelable

@Parcelize
@Serializable
data class DonationMethod(
    val name: String,
    val value: String,
    val type: String,
    val iconUrl: String
) : Parcelable