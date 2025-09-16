package kr.jiasoft.hiteen.feature.asset.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface AssetRepository : CoroutineCrudRepository<AssetEntity, Long> {

    suspend fun findByUid(uid: UUID): AssetEntity?

    @Query("""
        UPDATE assets 
        SET download_count = COALESCE(download_count,0) + 1, updated_id = :updatedId, updated_at = now()
        WHERE uid = :uid AND deleted_at IS NULL
        RETURNING *
    """)
    suspend fun increaseDownloadAndReturn(uid: UUID, updatedId: Long): AssetEntity?


    @Query("UPDATE assets SET download_count = download_count + 1 WHERE uid = :uid AND deleted_at IS NULL RETURNING *")
    suspend fun increaseDownload(uid: UUID): AssetEntity?

    @Query("""
        SELECT * FROM assets 
        WHERE deleted_at IS NULL 
        ORDER BY id DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun listAlive(limit: Int, offset: Int): Flow<AssetEntity>

    @Query("""
        UPDATE assets
        SET deleted_id = :deletedId, deleted_at = now()
        WHERE uid = :uid AND deleted_at IS NULL
        RETURNING *
    """)
    suspend fun softDelete(uid: UUID, deletedId: Long): AssetEntity?
}
