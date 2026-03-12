package com.methodica.app.data.repository

import com.methodica.app.data.local.dao.SubjectDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.data.local.entity.toEntity
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubjectRepositoryImpl(
    private val dao: SubjectDao
) : SubjectRepository {

    override fun observeSubjects(): Flow<List<Subject>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSubject(id: Long): Subject? =
        dao.getById(id)?.toDomain()

    override suspend fun saveSubject(subject: Subject) =
        dao.upsert(subject.toEntity())

    override suspend fun deleteSubject(subject: Subject) =
        dao.delete(subject.toEntity())
}
