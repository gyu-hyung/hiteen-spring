package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminSchoolResponse
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminSchoolRepository : CoroutineCrudRepository<SchoolEntity, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM schools s
        WHERE s.deleted_at IS NULL
            AND (:sido IS NULL OR :sido = 'ALL' OR s.sido = :sido)
            AND (:type IS NULL OR s.type = :type)
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (s.name ILIKE ('%' || :search || '%')
                         OR s.address ILIKE ('%' || :search || '%'))
                    WHEN :searchType = 'name' THEN
                        s.name ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'address' THEN
                        s.address ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
    """)
    suspend fun countSearchResults(
        sido: String?,
        type: Int?,
        searchType: String?,
        search: String?
    ): Int

    @Query("""
        SELECT 
            s.*,
            (SELECT COUNT(*) FROM users WHERE school_id = s.id AND deleted_at IS NULL) member_count,
            (SELECT COUNT(*) FROM school_classes WHERE school_id = s.id AND deleted_at IS NULL) class_count
        FROM schools s
        WHERE s.deleted_at IS NULL
            AND (:sido IS NULL OR :sido = 'ALL' OR s.sido = :sido)
            AND (:type IS NULL OR s.type = :type)
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (s.name ILIKE ('%' || :search || '%')
                         OR s.address ILIKE ('%' || :search || '%'))
                    WHEN :searchType = 'name' THEN
                        s.name ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'address' THEN
                        s.address ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        ORDER BY sido ASC, type ASC, name ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listSearchResults(
        sido: String?,
        type: Int?,
        searchType: String?,
        search: String?,
        limit: Int,
        offset: Int,
    ): Flow<AdminSchoolResponse>

    @Query("""
        SELECT MAX(CAST(SUBSTRING(code, 2) AS INTEGER))
        FROM schools
        WHERE code LIKE 'H%'
    """)
    suspend fun findMaxSchoolCodeNumber(): Int?

    // 학교의 회원수 체크
    // 해당 학교의 회원이 있으면 학교를 삭제하지 못하게 하기 위해..
    @Query("""
        SELECT COUNT(*)
        FROM users
        WHERE deleted_at IS NULL AND school_id = :schoolId
    """)
    suspend fun countSchoolUsers(schoolId: Long): Int
}