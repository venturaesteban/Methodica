package com.methodica.app.data.repository

import com.methodica.app.data.local.dao.AcademicYearDao
import com.methodica.app.data.local.dao.DegreeDao
import com.methodica.app.data.local.entity.AcademicYearEntity
import com.methodica.app.data.local.entity.DegreeEntity
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.repository.AcademicCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AcademicCatalogRepositoryImpl(
    private val degreeDao: DegreeDao,
    private val academicYearDao: AcademicYearDao
) : AcademicCatalogRepository {

    override fun observeDegrees(): Flow<List<Degree>> =
        degreeDao.observeAll().map { entities ->
            entities
                .map { it.toDomain() }
                .filter { degree ->
                    val normalized = degree.name.trim()
                    normalized.isNotEmpty() && !normalized.equals("Sin nombre", ignoreCase = true)
                }
        }

    override fun observeDegreesByStatus(status: DegreeStatus): Flow<List<Degree>> =
        degreeDao.observeByStatus(status.name).map { entities ->
            entities
                .map { it.toDomain() }
                .filter { degree ->
                    val normalized = degree.name.trim()
                    normalized.isNotEmpty() && !normalized.equals("Sin nombre", ignoreCase = true)
                }
        }

    override fun observeAcademicYears(degreeId: Long): Flow<List<AcademicYear>> =
        academicYearDao.observeByDegreeId(degreeId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDegree(id: Long): Degree? =
        degreeDao.getById(id)?.toDomain()

    override suspend fun getAcademicYear(id: Long): AcademicYear? =
        academicYearDao.getById(id)?.toDomain()

    override suspend fun upsertDegree(degree: Degree): Long {
        val normalizedName = degree.name.trim()
        require(normalizedName.isNotEmpty()) { "El nombre de la titulacion no puede estar vacío" }

        val id = degreeDao.upsert(
            DegreeEntity(
                id = degree.id,
                name = normalizedName,
                description = degree.description?.trim()?.ifEmpty { null },
                status = degree.status.name
            )
        )
        return if (id > 0L) id else degree.id
    }

    override suspend fun updateDegreeStatus(id: Long, status: DegreeStatus) {
        degreeDao.updateStatus(id, status.name)
    }

    override suspend fun deleteDegree(id: Long) {
        degreeDao.deleteById(id)
    }

    override suspend fun deleteAcademicYear(id: Long) {
        academicYearDao.deleteById(id)
    }

    override suspend fun ensureDegree(name: String): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "El nombre de la titulacion no puede estar vacío" }
        val existing = degreeDao.getByName(normalizedName)
        if (existing != null) return existing.id

        val id = degreeDao.upsert(DegreeEntity(name = normalizedName, status = DegreeStatus.ACTIVE.name))
        return if (id > 0L) id else checkNotNull(degreeDao.getByName(normalizedName)).id
    }

    override suspend fun ensureAcademicYear(degreeId: Long, yearNumber: Int): Long {
        val existing = academicYearDao.getByDegreeAndYearNumber(degreeId, yearNumber)
        if (existing != null) return existing.id

        val id = academicYearDao.upsert(
            AcademicYearEntity(
                degreeId = degreeId,
                yearNumber = yearNumber,
                name = "Curso $yearNumber"
            )
        )
        return if (id > 0L) id
        else checkNotNull(academicYearDao.getByDegreeAndYearNumber(degreeId, yearNumber)).id
    }
}

