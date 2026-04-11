package com.methodica.app.data.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.methodica.app.data.preferences.PlanningSettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settingsRepository = PlanningSettingsRepositoryImpl(context.applicationContext)
                val scheduler = ReminderScheduler(context.applicationContext)
                val remindersEnabled = settingsRepository.observeSettings().first().remindersEnabled
                if (remindersEnabled) scheduler.scheduleDaily() else scheduler.cancelDaily()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}
