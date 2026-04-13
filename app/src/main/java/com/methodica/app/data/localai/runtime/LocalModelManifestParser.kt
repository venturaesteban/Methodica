package com.methodica.app.data.localai.runtime

import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import com.methodica.app.domain.ai.local.DownloadableLocalModelManifest
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class LocalModelManifestParser @Inject constructor() {
    fun parse(rawJson: String, defaultMinSdk: Int): DownloadableLocalModelManifest {
        val root = JSONObject(rawJson)
        val manifestVersion = root.optString("manifestVersion", "1").ifBlank { "1" }
        val modelsJson = root.optJSONArray("models") ?: JSONArray()
        val models = buildList {
            for (index in 0 until modelsJson.length()) {
                val item = modelsJson.optJSONObject(index) ?: continue
                add(
                    DownloadableLocalModelDescriptor(
                        id = item.getString("id"),
                        version = item.getString("version"),
                        downloadUrl = item.getString("downloadUrl"),
                        sha256 = item.getString("sha256"),
                        sizeBytes = item.getLong("sizeBytes"),
                        requiredRamMb = item.getInt("requiredRamMb"),
                        requiredDiskBytes = item.getLong("requiredDiskBytes"),
                        supportedAbis = item.optJSONArray("supportedAbis").toStringList(),
                        minSdk = item.optInt("minSdk", defaultMinSdk),
                        noticeUrl = item.optString("noticeUrl").ifBlank { null },
                        termsUrl = item.optString("termsUrl").ifBlank { null },
                        prohibitedUsePolicyUrl = item.optString("prohibitedUsePolicyUrl").ifBlank { null }
                    )
                )
            }
        }
        return DownloadableLocalModelManifest(
            manifestVersion = manifestVersion,
            models = models
        )
    }
}

internal fun JSONArray?.toStringList(): List<String> = buildList {
    if (this@toStringList == null) return@buildList
    for (index in 0 until this@toStringList.length()) {
        add(this@toStringList.optString(index))
    }
}.filter { it.isNotBlank() }
