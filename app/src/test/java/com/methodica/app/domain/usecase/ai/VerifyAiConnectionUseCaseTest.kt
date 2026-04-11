package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.ai.LlmProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyAiConnectionUseCaseTest {

    private lateinit var useCase: VerifyAiConnectionUseCase

    @Before
    fun setUp() {
        useCase = VerifyAiConnectionUseCase(FakeLlmProvider())
    }

    @Test
    fun `invoke should return success when provider responds correctly`() = runTest {
        // Arrange
        val baseUrl = "https://api.openai.com/v1/chat/completions"
        val model = "gpt-4o-mini"
        val apiKey = "test-api-key"

        // Act
        val result = useCase(baseUrl, model, apiKey)

        // Assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("OK", result.getOrNull())
    }

    @Test
    fun `invoke should return failure when baseUrl is blank`() = runTest {
        // Arrange
        val baseUrl = ""
        val model = "gpt-4o-mini"
        val apiKey = "test-api-key"

        // Act
        val result = useCase(baseUrl, model, apiKey)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke should return failure when model is blank`() = runTest {
        // Arrange
        val baseUrl = "https://api.openai.com/v1/chat/completions"
        val model = ""
        val apiKey = "test-api-key"

        // Act
        val result = useCase(baseUrl, model, apiKey)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke should return failure when apiKey is blank`() = runTest {
        // Arrange
        val baseUrl = "https://api.openai.com/v1/chat/completions"
        val model = "gpt-4o-mini"
        val apiKey = ""

        // Act
        val result = useCase(baseUrl, model, apiKey)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke should return failure when provider throws connection error`() = runTest {
        val failingUseCase = VerifyAiConnectionUseCase(
            object : LlmProvider {
                override suspend fun generate(
                    baseUrl: String,
                    model: String,
                    apiKey: String,
                    prompt: String
                ): String? {
                    throw IllegalStateException("Error HTTP 401")
                }
            }
        )

        val result = failingUseCase(
            baseUrl = "https://api.openai.com/v1/chat/completions",
            model = "gpt-4o-mini",
            apiKey = "bad-key"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    private class FakeLlmProvider : LlmProvider {
        override suspend fun generate(
            baseUrl: String,
            model: String,
            apiKey: String,
            prompt: String
        ): String? = "OK"
    }
}

