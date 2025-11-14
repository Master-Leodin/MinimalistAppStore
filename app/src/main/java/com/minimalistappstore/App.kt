package com.minimalistappstore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class App(
    val nameKey: String = "",
    val developer: String = "",
    val descriptionKey: String = "",
    val iconUrl: String = "",
    val apkUrl: String = "",
    val version: String = "",
    val latestVersionCode: Int = 0,
    val openSourceUrl: String = "",
    val packageName: String = "",
    val screenshotUrls: List<String> = emptyList()
) : Parcelable {

    fun getTranslatedName(context: android.content.Context): String {
        return try {
            val resourceId = context.resources.getIdentifier(nameKey, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                nameKey // Fallback
            }
        } catch (e: Exception) {
            nameKey // Fallback
        }
    }

    fun getTranslatedDescription(context: android.content.Context): String {
        return try {
            val resourceId =
                context.resources.getIdentifier(descriptionKey, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                descriptionKey // Fallback
            }
        } catch (e: Exception) {
            descriptionKey // Fallback
        }
    }
}