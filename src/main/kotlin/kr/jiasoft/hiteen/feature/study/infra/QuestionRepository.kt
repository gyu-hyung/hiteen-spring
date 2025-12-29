package kr.jiasoft.hiteen.feature.study.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface QuestionRepository : CoroutineCrudRepository<QuestionEntity, Long> {

    // 무작위로 N개 단어 가져오기
    @Query("SELECT * FROM question_2 WHERE status = 1 ORDER BY RAND() LIMIT :limit")
    fun findRandomQuestions(limit: Int): Flow<QuestionEntity>

    @Query("SELECT * FROM question_2 WHERE type = :type AND status = 1")
    fun findByType(type: Int): Flow<QuestionEntity>

    fun findByQuestionAndDeletedAtIsNull(question: String): Flow<QuestionEntity?>

    @Query("""
        SELECT *
        FROM question_2
        WHERE LOWER(question) = LOWER(:question)
          AND deleted_at IS NULL
          AND type = :type
    """)
    fun findByLowCaseQuestionAndDeletedAtIsNull(question: String, type: Int): Flow<QuestionEntity>


    @Query("select * from question_2 where sound is null")
    fun findBySoundIsNull(): Flow<QuestionEntity>

}
