package kr.jiasoft.hiteen.feature.inquiry.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.inquiry.domain.InquiryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : CoroutineCrudRepository<InquiryEntity, Long> {

    @Query("""
        SELECT *
        FROM inquiries
        WHERE (:status IS NULL OR :status = 'ALL' OR status = :status)
          AND (:search IS NULL 
               OR name ILIKE CONCAT('%', :search, '%') 
               OR phone ILIKE CONCAT('%', :search, '%')
               OR email ILIKE CONCAT('%', :search, '%')
               OR content ILIKE CONCAT('%', :search, '%'))
        ORDER BY
            CASE WHEN :order = 'DESC' THEN created_at END DESC,
            CASE WHEN :order = 'ASC' THEN created_at END ASC
        LIMIT :size OFFSET GREATEST((:page - 1) * :size, 0)
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        status: String?,
        search: String?,
    ): Flow<InquiryEntity>

    @Query("""
        SELECT COUNT(*)
        FROM inquiries
        WHERE (:status IS NULL OR :status = 'ALL' OR status = :status)
          AND (:search IS NULL 
               OR name ILIKE CONCAT('%', :search, '%') 
               OR phone ILIKE CONCAT('%', :search, '%')
               OR email ILIKE CONCAT('%', :search, '%')
               OR content ILIKE CONCAT('%', :search, '%'))
    """)
    suspend fun totalCount(
        status: String?,
        search: String?,
    ): Int
}

