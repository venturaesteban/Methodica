package com.methodica.app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val dbName = "migration-test-methodica.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To7_keepsSchemaValidAndNormalizesAcademicHierarchy() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                """
                INSERT INTO subjects (id, name, colorHex, description)
                VALUES (1, 'Matematicas', '#FF5722', 'Base')
                """.trimIndent()
            )
            close()
        }

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()

        db.openHelper.writableDatabase

        val cursor = db.query(
            SimpleSQLiteQuery(
                """
                SELECT d.name, y.yearNumber
                FROM subjects s
                INNER JOIN academic_years y ON y.id = s.academicYearId
                INNER JOIN degrees d ON d.id = y.degreeId
                WHERE s.id = 1
                """.trimIndent()
            )
        )
        cursor.use {
            assertEquals(true, it.moveToFirst())
            assertEquals("Sin titulacion", it.getString(0))
            assertEquals(1, it.getInt(1))
        }

        db.close()
    }
}
