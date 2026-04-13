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
    fun migrate2To10_keepsSchemaValidAndCreatesLocalAiTables() {
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

        val localAiTables = db.query(
            SimpleSQLiteQuery(
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                  AND name IN ('ai_document_chunks', 'ai_chunk_embeddings', 'ai_indexing_runs', 'local_ai_model_state')
                ORDER BY name
                """.trimIndent()
            )
        )
        localAiTables.use {
            assertEquals(true, it.moveToFirst())
            val found = mutableListOf<String>()
            do {
                found += it.getString(0)
            } while (it.moveToNext())
            assertEquals(
                listOf("ai_chunk_embeddings", "ai_document_chunks", "ai_indexing_runs", "local_ai_model_state"),
                found
            )
        }

        val localStateRows = db.query(
            SimpleSQLiteQuery(
                """
                SELECT COUNT(1)
                FROM pragma_table_info('local_ai_model_state')
                WHERE name IN (
                    'displayName',
                    'supportedAbisCsv',
                    'minSdk',
                    'downloadUrl',
                    'expectedSha256',
                    'downloadedBytes',
                    'totalBytes'
                )
                """.trimIndent()
            )
        )
        localStateRows.use {
            assertEquals(true, it.moveToFirst())
            assertEquals(7, it.getInt(0))
        }

        db.close()
    }
}
