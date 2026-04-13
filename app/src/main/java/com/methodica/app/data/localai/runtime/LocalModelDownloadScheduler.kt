package com.methodica.app.data.localai.runtime

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.methodica.app.domain.ai.local.LocalAiModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelDownloadScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun enqueue(type: LocalAiModelType) {
        val request = OneTimeWorkRequestBuilder<LocalModelDownloadWorker>()
            .setInputData(workDataOf(LocalModelDownloadWorker.KEY_MODEL_TYPE to type.name))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(type.uniqueDownloadWorkName())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            type.uniqueDownloadWorkName(),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(type: LocalAiModelType) {
        WorkManager.getInstance(context).cancelUniqueWork(type.uniqueDownloadWorkName())
    }
}
