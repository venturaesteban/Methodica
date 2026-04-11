package com.methodica.app.domain.repository

import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus
import kotlinx.coroutines.flow.Flow

interface AcademicCatalogRepository {
    fun observeDegrees(): Flow<List<Degree>>
    fun observeDegreesByStatus(status: DegreeStatus): Flow<List<Degree>>
    fun observeAcademicYears(degreeId: Long): Flow<List<AcademicYear>>
    suspend fun getDegree(id: Long): Degree?
    suspend fun getAcademicYear(id: Long): AcademicYear?
    suspend fun upsertDegree(degree: Degree): Long
    suspend fun updateDegreeStatus(id: Long, status: DegreeStatus)
    suspend fun deleteDegree(id: Long)
    suspend fun deleteAcademicYear(id: Long)
    suspend fun ensureDegree(name: String): Long
    suspend fun ensureAcademicYear(degreeId: Long, yearNumber: Int): Long
}

