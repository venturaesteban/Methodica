package com.methodica.app.domain.repository

import com.methodica.app.domain.model.Subject
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de materias definido en la capa domain.
 * La implementación concreta vive en data/repository y depende de Room,
 * lo que mantiene la capa domain libre de detalles de persistencia.
 */
interface SubjectRepository {
    fun observeSubjects(): Flow<List<Subject>>
    suspend fun getSubject(id: Long): Subject?
    suspend fun saveSubject(subject: Subject)
    suspend fun deleteSubject(subject: Subject)
}
