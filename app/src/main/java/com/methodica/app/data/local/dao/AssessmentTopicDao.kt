package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.methodica.app.data.local.entity.AssessmentTopicCrossRef
import com.methodica.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentTopicDao {

    @Query(
        """
        SELECT t.* FROM topics t
        INNER JOIN assessment_topic_cross_ref ref ON ref.topicId = t.id
        WHERE ref.assessmentId = :assessmentId
        ORDER BY t.`order` ASC
        """
    )
    fun observeTopicsForAssessment(assessmentId: Long): Flow<List<TopicEntity>>

    @Query(
        """
        SELECT t.* FROM topics t
        INNER JOIN assessment_topic_cross_ref ref ON ref.topicId = t.id
        WHERE ref.assessmentId = :assessmentId
        ORDER BY t.`order` ASC
        """
    )
    suspend fun getTopicsForAssessment(assessmentId: Long): List<TopicEntity>

    @Query("SELECT topicId FROM assessment_topic_cross_ref WHERE assessmentId = :assessmentId")
    fun observeTopicIdsForAssessment(assessmentId: Long): Flow<List<Long>>

    @Query("DELETE FROM assessment_topic_cross_ref WHERE assessmentId = :assessmentId")
    suspend fun deleteAllForAssessment(assessmentId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<AssessmentTopicCrossRef>)

    @Transaction
    suspend fun replaceTopicsForAssessment(assessmentId: Long, topicIds: List<Long>) {
        deleteAllForAssessment(assessmentId)
        if (topicIds.isNotEmpty()) {
            insertAll(topicIds.map { AssessmentTopicCrossRef(assessmentId, it) })
        }
    }
}
