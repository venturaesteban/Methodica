package com.methodica.app.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleDaily() {
        val initialDelayMillis = calculateInitialDelayMillis(targetHour = 8)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(ReminderWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDaily() {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.UNIQUE_WORK_NAME)
    }

    private fun calculateInitialDelayMillis(targetHour: Int): Long {
        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val nextRun = now
            .withHour(targetHour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .let { scheduled -> if (scheduled.isAfter(now)) scheduled else scheduled.plusDays(1) }

        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
    }
}
