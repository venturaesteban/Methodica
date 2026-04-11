package com.methodica.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val KEY_REMINDERS_ENABLED  = booleanPreferencesKey("planning_reminders_enabled")
        val KEY_STUDENT_AGE        = intPreferencesKey("planning_student_age")
        val KEY_STUDENT_DEGREE_ID  = longPreferencesKey("planning_student_degree_id")
        val KEY_STUDENT_CURRENT_DEGREE_IDS = stringSetPreferencesKey("planning_student_current_degree_ids")
        val KEY_STUDENT_COMPLETED_DEGREE_IDS = stringSetPreferencesKey("planning_student_completed_degree_ids")
        val KEY_READING_LEVEL      = intPreferencesKey("planning_reading_level")
        val KEY_PASSED_SUBJECT_IDS = stringSetPreferencesKey("planning_passed_subject_ids")
    }

    override fun observeSettings(): Flow<PlanningSettings> =
        dataStore.data.map { prefs ->
            val currentDegreeIds = prefs[KEY_STUDENT_CURRENT_DEGREE_IDS]
                ?.mapNotNull { value -> value.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            val completedDegreeIds = prefs[KEY_STUDENT_COMPLETED_DEGREE_IDS]
                ?.mapNotNull { value -> value.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            val legacyDegreeId = prefs[KEY_STUDENT_DEGREE_ID]
            val resolvedCurrentDegreeIds = if (currentDegreeIds.isNotEmpty() || legacyDegreeId == null) {
                currentDegreeIds
            } else {
                setOf(legacyDegreeId)
            }
            PlanningSettings(
                availableHoursPerDay = prefs[KEY_HOURS_PER_DAY] ?: 4,
                unavailableWeekdays  = prefs[KEY_UNAVAILABLE_DAYS]
                    ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                    ?.toSet()
                    ?: emptySet(),
                bufferDaysBeforeExam = prefs[KEY_BUFFER_DAYS] ?: 2,
                finalReviewDays      = prefs[KEY_FINAL_REVIEW_DAYS] ?: 1,
                remindersEnabled     = prefs[KEY_REMINDERS_ENABLED] ?: true,
                studentAge           = prefs[KEY_STUDENT_AGE] ?: 20,
                studentCurrentDegreeIds = resolvedCurrentDegreeIds,
                studentCompletedDegreeIds = completedDegreeIds,
                readingComprehensionLevel = prefs[KEY_READING_LEVEL] ?: 3,
                passedSubjectIds     = prefs[KEY_PASSED_SUBJECT_IDS]
                    ?.mapNotNull { value -> value.toLongOrNull() }
                    ?.toSet()
                    ?: emptySet()
            )
        }

    override suspend fun saveSettings(settings: PlanningSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_HOURS_PER_DAY]     = settings.availableHoursPerDay
            prefs[KEY_UNAVAILABLE_DAYS]  = settings.unavailableWeekdays.map { it.name }.toSet()
            prefs[KEY_BUFFER_DAYS]       = settings.bufferDaysBeforeExam
            prefs[KEY_FINAL_REVIEW_DAYS] = settings.finalReviewDays
            prefs[KEY_REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[KEY_STUDENT_AGE]       = settings.studentAge
            prefs[KEY_STUDENT_CURRENT_DEGREE_IDS] = settings.studentCurrentDegreeIds.map { it.toString() }.toSet()
            prefs[KEY_STUDENT_COMPLETED_DEGREE_IDS] = settings.studentCompletedDegreeIds.map { it.toString() }.toSet()
            prefs.remove(KEY_STUDENT_DEGREE_ID)
            prefs[KEY_READING_LEVEL]      = settings.readingComprehensionLevel
            prefs[KEY_PASSED_SUBJECT_IDS] = settings.passedSubjectIds.map { it.toString() }.toSet()
        }
    }
}
