package com.methodica.app.data.work

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.methodica.app.MainActivity
import com.methodica.app.R
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.local.AppDatabaseMigrations
import com.methodica.app.data.preferences.PlanningSettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class ReminderWorker(
    context: Context,
    params:  WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = PlanningSettingsRepositoryImpl(applicationContext).observeSettings().first()
        if (!settings.remindersEnabled) return Result.success()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "methodica.db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .fallbackToDestructiveMigration()
            .build()

        return try {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val tomorrowMillis = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayAfterTomorrowMillis = tomorrow.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val sessionsToday = database.studySessionDao().getByDate(todayMillis)
            val upcomingAssessments = database.assessmentDao().getByDateRange(tomorrowMillis, dayAfterTomorrowMillis)

            if (sessionsToday.isEmpty() && upcomingAssessments.isEmpty()) {
                return Result.success()
            }

            postNotification(
                sessionsCount = sessionsToday.size,
                assessmentsCount = upcomingAssessments.size
            )
            Result.success()
        } finally {
            database.close()
        }
    }

    private fun postNotification(sessionsCount: Int, assessmentsCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = NotificationManagerCompat.from(applicationContext)
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Recordatorios académicos",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val launchIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = buildString {
            if (sessionsCount > 0) append("$sessionsCount sesiones hoy")
            if (sessionsCount > 0 && assessmentsCount > 0) append(" • ")
            if (assessmentsCount > 0) append("$assessmentsCount evaluaciones próximas")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Methodica")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "methodica_daily_reminder"
        const val WORK_TAG = "methodica_reminders"
        private const val CHANNEL_ID = "methodica_reminders_channel"
        private const val NOTIFICATION_ID = 7001
    }
}
