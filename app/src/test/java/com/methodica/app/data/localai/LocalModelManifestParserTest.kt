package com.methodica.app.data.localai

import com.methodica.app.data.localai.runtime.LocalModelManifestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelManifestParserTest {

    private val parser = LocalModelManifestParser()

    @Test
    fun `parsea campos legales y de distribucion del manifest remoto`() {
        val manifest = parser.parse(
            rawJson = """
                {
                  "manifestVersion": "1",
                  "models": [
                    {
                      "id": "gemma-3n-e2b-it-int4-litertlm",
                      "version": "gemma-3n-e2b-it-int4-litertlm-v1",
                      "downloadUrl": "https://storage.googleapis.com/methodica-bucket/models/gemma-3n-E2B-it-int4.litertlm",
                      "sha256": "abc123",
                      "sizeBytes": 3655827456,
                      "requiredRamMb": 4096,
                      "requiredDiskBytes": 4831838208,
                      "supportedAbis": ["arm64-v8a"],
                      "minSdk": 26,
                      "noticeUrl": "https://storage.googleapis.com/methodica-bucket/legal/NOTICE_GEMMA.txt",
                      "termsUrl": "https://ai.google.dev/gemma/terms",
                      "prohibitedUsePolicyUrl": "https://ai.google.dev/gemma/prohibited_use_policy"
                    }
                  ]
                }
            """.trimIndent(),
            defaultMinSdk = 24
        )

        assertEquals("1", manifest.manifestVersion)
        assertEquals(1, manifest.models.size)
        val model = manifest.models.single()
        assertEquals("gemma-3n-e2b-it-int4-litertlm", model.id)
        assertEquals("gemma-3n-e2b-it-int4-litertlm-v1", model.version)
        assertEquals("https://storage.googleapis.com/methodica-bucket/models/gemma-3n-E2B-it-int4.litertlm", model.downloadUrl)
        assertEquals("abc123", model.sha256)
        assertEquals(3655827456, model.sizeBytes)
        assertEquals(4096, model.requiredRamMb)
        assertEquals(4831838208, model.requiredDiskBytes)
        assertEquals(26, model.minSdk)
        assertEquals("https://storage.googleapis.com/methodica-bucket/legal/NOTICE_GEMMA.txt", model.noticeUrl)
        assertEquals("https://ai.google.dev/gemma/terms", model.termsUrl)
        assertEquals("https://ai.google.dev/gemma/prohibited_use_policy", model.prohibitedUsePolicyUrl)
        assertTrue(model.supportedAbis.contains("arm64-v8a"))
    }
}
