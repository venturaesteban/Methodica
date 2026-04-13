package com.methodica.app

import android.app.Application
import com.methodica.app.data.work.ReminderScheduler
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.repository.PlanningSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class MethodicaApplication : Application() {

	@Inject
	lateinit var planningSettingsRepository: PlanningSettingsRepository

	@Inject
	lateinit var reminderScheduler: ReminderScheduler

	@Inject
	lateinit var localModelRuntimeManager: LocalModelRuntimeManager

	private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	override fun onCreate() {
		super.onCreate()
		applicationScope.launch {
			val remindersEnabled = planningSettingsRepository.observeSettings().first().remindersEnabled
			if (remindersEnabled) reminderScheduler.scheduleDaily() else reminderScheduler.cancelDaily()
		}
		applicationScope.launch {
			localModelRuntimeManager.refreshDownloadableModels()
			localModelRuntimeManager.prepareAutomaticModels()
		}
	}
}
