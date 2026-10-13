package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model representing app update information hosted on GitHub / GitHub Pages.
 */
@JsonClass(generateAdapter = true)
data class AppUpdateInfo(
    @Json(name = "versionCode")
    val versionCode: Int,

    @Json(name = "versionName")
    val versionName: String,

    @Json(name = "apkUrl")
    val apkUrl: String,

    @Json(name = "releaseNotes")
    val releaseNotes: String,

    @Json(name = "minimumSupportedVersion")
    val minimumSupportedVersion: Int = 1,

    @Json(name = "fileSize")
    val fileSize: Long = 0L,

    @Json(name = "sha256")
    val sha256: String = ""
)
