package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.SubjectEntity
import com.methodica.app.data.local.entity.SubjectWithHierarchyRow
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query(
        """
        SELECT
            s.id AS id,
            s.academicYearId AS academicYearId,
            y.degreeId AS degreeId,
            d.name AS degreeName,
            y.yearNumber AS courseYear,
            s.name AS name,
            s.colorHex AS colorHex,
            s.description AS description
        FROM subjects s
        INNER JOIN academic_years y ON y.id = s.academicYearId
        INNER JOIN degrees d ON d.id = y.degreeId
        ORDER BY d.name ASC, y.yearNumber ASC, s.name ASC
        """
    )
    fun observeAll(): Flow<List<SubjectWithHierarchyRow>>

    @Query(
        """
        SELECT
            s.id AS id,
            s.academicYearId AS academicYearId,
            y.degreeId AS degreeId,
            d.name AS degreeName,
            y.yearNumber AS courseYear,
            s.name AS name,
            s.colorHex AS colorHex,
            s.description AS description
        FROM subjects s
        INNER JOIN academic_years y ON y.id = s.academicYearId
        INNER JOIN degrees d ON d.id = y.degreeId
        WHERE s.academicYearId = :academicYearId
        ORDER BY s.name ASC
        """
    )
    fun observeByAcademicYearId(academicYearId: Long): Flow<List<SubjectWithHierarchyRow>>

    @Query(
        """
        SELECT
            s.id AS id,
            s.academicYearId AS academicYearId,
            y.degreeId AS degreeId,
            d.name AS degreeName,
            y.yearNumber AS courseYear,
            s.name AS name,
            s.colorHex AS colorHex,
            s.description AS description
        FROM subjects s
        INNER JOIN academic_years y ON y.id = s.academicYearId
        INNER JOIN degrees d ON d.id = y.degreeId
        WHERE s.id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: Long): SubjectWithHierarchyRow?

    /** Inserta o actualiza una materia. @Upsert disponible desde Room 2.5 */
    @Upsert
    suspend fun upsert(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)
}
