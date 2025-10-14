package kr.jiasoft.hiteen.feature.study.infra

import kr.jiasoft.hiteen.feature.study.domain.StudyEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface StudyRepository : CoroutineCrudRepository<StudyEntity, Long> {
    suspend fun findAllByUserId(userId: Long): List<StudyEntity>
    suspend fun findByUid(uid: String): StudyEntity?
    suspend fun findByUidAndUserId(uid: String, userId: Long): StudyEntity?

    @Query("""
        SELECT * FROM study 
        WHERE user_id = :userId 
          AND season_id = :seasonId 
          AND status = 1 
          AND deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun findOngoingStudy(userId: Long, seasonId: Long): StudyEntity?

}
