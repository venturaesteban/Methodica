package com.methodica.app.data.localai.runtime

import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedLocalModelArtifactAccess(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)

@Singleton
class LocalModelArtifactAccessResolver @Inject constructor() {
    suspend fun resolve(descriptor: DownloadableLocalModelDescriptor): ResolvedLocalModelArtifactAccess =
        ResolvedLocalModelArtifactAccess(url = descriptor.downloadUrl)
}
