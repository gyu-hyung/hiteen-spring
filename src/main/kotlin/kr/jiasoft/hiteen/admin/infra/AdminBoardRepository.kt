package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kotlinx.coroutines.flow.Flow

interface AdminBoardRepository {

    suspend fun save(entity: BoardEntity): BoardEntity

    suspend fun findById(id: Long): BoardEntity?

    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        category: String?,
    ): Flow<BoardEntity>

    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        category: String?,
    ): Int
}
