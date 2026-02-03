package kr.jiasoft.hiteen.feature.map.infra

import kr.jiasoft.hiteen.feature.map.domain.ApiKeyEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ApiKeyRepository : CoroutineCrudRepository<ApiKeyEntity, Long> {

    @Query(
        """
        SELECT api_key_id
        FROM api_keys
        WHERE (:type IS NULL OR type = :type)
          AND status = :status
          AND deleted_at IS NULL
        ORDER BY random()
        LIMIT 1
        """
    )
    suspend fun findRandomApiKeyId(type: String?, status: String = "ACTIVE"): String?
}

