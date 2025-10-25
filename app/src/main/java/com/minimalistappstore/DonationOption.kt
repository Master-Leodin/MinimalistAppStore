// DonationOption.kt
package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DonationOption(
    val title: String,
    val subtitle: String,
    val description: String,
    val methods: List<DonationMethod>
) : Parcelable

@Parcelize
data class DonationMethod(
    val name: String,
    val value: String,
    val type: String // "link" para abrir no navegador, "text" para copiar
) : Parcelable