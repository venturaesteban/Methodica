package com.methodica.app.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker base para recordatorios futuros.
 *
 * En Fase 0 no contiene lógica de negocio; solo verifica que WorkManager
 * puede ser instanciado y ejecutado. La lógica de programación de recordatorios
 * académicos se implementará en Fase 2.
 *
 * WorkManager se auto-inicializa vía AndroidX Startup; no es necesario
 * llamar a WorkManager.initialize() en Application.
 */
class ReminderWorker(
    context: Context,
    params:  WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = Result.success()
}
