package com.methodica.app.data.localai.runtime

import com.methodica.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelManifestConfig @Inject constructor() {
    fun configuredManifestUrl(): String = BuildConfig.LOCAL_MODEL_MANIFEST_URL.trim()
}
