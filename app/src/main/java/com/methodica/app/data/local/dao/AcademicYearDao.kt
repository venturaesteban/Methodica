package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.AcademicYearEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicYearDao {

    @Query("SELECT * FROM academic_years WHERE degreeId = :degreeId ORDER BY yearNumber ASC")
    fun observeByDegreeId(degreeId: Long): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE degreeId = :degreeId AND yearNumber = :yearNumber LIMIT 1")
    suspend fun getByDegreeAndYearNumber(degreeId: Long, yearNumber: Int): AcademicYearEntity?

    @Query("DELETE FROM academic_years WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Upsert
    suspend fun upsert(academicYear: AcademicYearEntity): Long
}

