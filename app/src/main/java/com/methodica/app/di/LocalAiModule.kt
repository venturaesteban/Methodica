package com.methodica.app.di

import com.methodica.app.data.ai.workflow.DefaultAiWorkflowCoordinator
import com.methodica.app.data.localai.provider.DeferredActionProvider
import com.methodica.app.data.localai.provider.DeferredEmbeddingProvider
import com.methodica.app.data.localai.provider.DeferredReasoningProvider
import com.methodica.app.data.localai.provider.ParagraphChunkingStrategy
import com.methodica.app.data.localai.provider.RoomBackedRetrievalIndex
import com.methodica.app.data.localai.runtime.RoomBackedLocalModelRuntimeManager
import com.methodica.app.domain.ai.local.ActionProvider
import com.methodica.app.domain.ai.local.ChunkingStrategy
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.workflow.AiWorkflowCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalAiModule {

    @Provides
    @Singleton
    fun provideLocalModelRuntimeManager(impl: RoomBackedLocalModelRuntimeManager): LocalModelRuntimeManager = impl

    @Provides
    @Singleton
    fun provideEmbeddingProvider(impl: DeferredEmbeddingProvider): EmbeddingProvider = impl

    @Provides
    @Singleton
    fun provideReasoningProvider(impl: DeferredReasoningProvider): ReasoningProvider = impl

    @Provides
    @Singleton
    fun provideActionProvider(impl: DeferredActionProvider): ActionProvider = impl

    @Provides
    @Singleton
    fun provideRetrievalIndex(impl: RoomBackedRetrievalIndex): RetrievalIndex = impl

    @Provides
    @Singleton
    fun provideChunkingStrategy(impl: ParagraphChunkingStrategy): ChunkingStrategy = impl

    @Provides
    @Singleton
    fun provideAiWorkflowCoordinator(impl: DefaultAiWorkflowCoordinator): AiWorkflowCoordinator = impl
}
