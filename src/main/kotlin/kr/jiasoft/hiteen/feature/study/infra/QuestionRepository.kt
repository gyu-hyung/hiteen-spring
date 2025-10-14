package kr.jiasoft.hiteen.feature.study.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface QuestionRepository : CoroutineCrudRepository<QuestionEntity, Long> {

    // 무작위로 N개 단어 가져오기
    @Query("SELECT * FROM question WHERE status = 1 ORDER BY RAND() LIMIT :limit")
    fun findRandomQuestions(limit: Int): Flow<QuestionEntity>

    @Query("SELECT * FROM question WHERE type = :type AND status = 1")
    fun findByType(type: Int): Flow<QuestionEntity>

}
