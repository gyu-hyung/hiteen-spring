package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminBoardRepository : CoroutineCrudRepository<BoardEntity, Long> {



    @Query("""
        SELECT COUNT(*)
        FROM question_2 q
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        q.question ILIKE CONCAT('%', :search, '%')
                        OR q.answer ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND q.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'answer' AND q.answer ILIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND q.status = 1)
                OR (:status = 'INACTIVE' AND q.status = 0)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR (:type = '1' AND q.type = 1)
                OR (:type = '2' AND q.type = 2)
                OR (:type = '3' AND q.type = 3)
            )
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        category: String?,
    ): Flow<BoardEntity>


    
    @Query("""
        SELECT q.*
        FROM question_2 q
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        q.question ILIKE CONCAT('%', :search, '%')
                        OR q.answer ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND q.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'answer' AND q.answer ILIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND q.status = 1)
                OR (:status = 'INACTIVE' AND q.status = 0)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR (:type = '1' AND q.type = 1)
                OR (:type = '2' AND q.type = 2)
                OR (:type = '3' AND q.type = 3)
            )
        ORDER BY
            q.status DESC,
            CASE WHEN :order = 'DESC' THEN q.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN q.created_at END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        category: String?,
    ): Int
}
