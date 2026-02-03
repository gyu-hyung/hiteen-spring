package kr.jiasoft.hiteen.feature.study.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.study.domain.QuestionItemsEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface QuestionItemsRepository : CoroutineCrudRepository<QuestionItemsEntity, Long> {

    @Query("""
        SELECT * FROM question_items 
        WHERE season_id = :seasonId 
          AND type = :type
        ORDER BY id ASC
    """)
    fun findAllBySeasonIdAndType(seasonId: Long, type: Int): Flow<QuestionItemsEntity>

    @Query("""
        SELECT * FROM question_items 
        WHERE season_id = :seasonId 
        ORDER BY id ASC
    """)
    fun findAllBySeasonId(seasonId: Long): Flow<QuestionItemsEntity>

}
