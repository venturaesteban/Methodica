package com.methodica.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.repository.PlanningSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class PlanningSettingsRepositoryImpl(
    private val context: Context
) : PlanningSettingsRepository {

    private val dataStore = context.userPreferencesDataStore

    private companion object {
        val KEY_HOURS_PER_DAY      = intPreferencesKey("planning_hours_per_day")
        val KEY_UNAVAILABLE_DAYS   = stringSetPreferencesKey("planning_unavailable_days")
        val KEY_BUFFER_DAYS        = intPreferencesKey("planning_buffer_days")
        val KEY_FINAL_REVIEW_DAYS  = intPreferencesKey("planning_final_review_days")
    }

    override fun observeSettings(): Flow<PlanningSettings> =
        dataStore.data.map { prefs ->
            PlanningSettings(
                availableHoursPerDay = prefs[KEY_HOURS_PER_DAY] ?: 4,
                unavailableWeekdays  = prefs[KEY_UNAVAILABLE_DAYS]
                    ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                    ?.toSet()
                    ?: emptySet(),
                bufferDaysBeforeExam = prefs[KEY_BUFFER_DAYS] ?: 2,
                finalReviewDays      = prefs[KEY_FINAL_REVIEW_DAYS] ?: 1
            )
        }

    override suspend fun saveSettings(settings: PlanningSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_HOURS_PER_DAY]     = settings.availableHoursPerDay
            prefs[KEY_UNAVAILABLE_DAYS]  = settings.unavailableWeekdays.map { it.name }.toSet()
            prefs[KEY_BUFFER_DAYS]       = settings.bufferDaysBeforeExam
            prefs[KEY_FINAL_REVIEW_DAYS] = settings.finalReviewDays
        }
    }
}
