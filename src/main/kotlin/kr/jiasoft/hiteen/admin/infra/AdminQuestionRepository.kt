package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminQuestionRepository : CoroutineCrudRepository<QuestionEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
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
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
    ): Int




    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
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
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
    ): Flow<QuestionEntity>



}