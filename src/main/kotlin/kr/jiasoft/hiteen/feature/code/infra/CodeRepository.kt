package kr.jiasoft.hiteen.feature.code.infra

import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Flux

interface CodeRepository : CoroutineCrudRepository<CodeEntity, Long> {

    @Cacheable(cacheNames = ["code"], key = "#group")
    @Query("""
        SELECT c.*, ca.uid
        FROM codes c
        left join code_assets ca on c.id = ca.code_id 
        WHERE deleted_at IS null
        AND c.status = 'ACTIVE'
        AND (
            :group IS NULL
            OR c.code_group = :group
            OR c.code_group ILIKE CONCAT(:group, '%')
        )
        ORDER BY code_group, code
    """)
    fun findByGroup(group: String?): Flux<CodeWithAssetResponse>

    @Query("SELECT code FROM codes WHERE code_group = :group ORDER BY code DESC LIMIT 1")
    suspend fun findLastCodeByGroup(group: String): String?



    @Query("""
        SELECT c.*, ca.uid
        FROM codes c
        left join code_assets ca on c.id = ca.code_id
        WHERE c.deleted_at IS NULL
          AND (
              :group IS NULL
              OR c.code_group = :group
              OR c.code_group ILIKE CONCAT(:group, '%')
          )
          AND (:status IS NULL OR c.status = :status)
          AND (
              :search IS NULL
              OR :search = ''
              OR (
                  (:searchType = 'CODE' AND c.code ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'NAME' AND c.code_name ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'GROUP' AND c.code_group ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'ALL' AND (
                      c.code ILIKE CONCAT('%', :search, '%')
                      OR c.code_name ILIKE CONCAT('%', :search, '%')
                      OR c.code_group ILIKE CONCAT('%', :search, '%')
                  ))
              )
          )
        ORDER BY
          CASE WHEN :order = 'ASC' THEN c.id END ASC,
          CASE WHEN :order = 'DESC' THEN c.id END DESC
        LIMIT :size OFFSET :offset
    """)
    fun listByPage(
        group: String?,
        status: String?,
        search: String?,
        searchType: String,
        order: String,
        size: Int,
        offset: Long,
    ): Flux<CodeWithAssetResponse>

    @Query("""
        SELECT COUNT(1)
        FROM codes c
        WHERE c.deleted_at IS NULL
          AND (
              :group IS NULL
              OR c.code_group = :group
              OR c.code_group ILIKE CONCAT(:group, '%')
          )
          AND (:status IS NULL OR c.status = :status)
          AND (
              :search IS NULL
              OR :search = ''
              OR (
                  (:searchType = 'CODE' AND c.code ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'NAME' AND c.code_name ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'GROUP' AND c.code_group ILIKE CONCAT('%', :search, '%'))
                  OR (:searchType = 'ALL' AND (
                      c.code ILIKE CONCAT('%', :search, '%')
                      OR c.code_name ILIKE CONCAT('%', :search, '%')
                      OR c.code_group ILIKE CONCAT('%', :search, '%')
                  ))
              )
          )
    """)
    suspend fun totalCount(
        group: String?,
        status: String?,
        search: String?,
        searchType: String,
    ): Int

    @Query("""
        SELECT c.code_group
        FROM codes c
        WHERE c.deleted_at IS NULL
        GROUP BY c.code_group
        ORDER BY c.code_group
    """)
    fun findGroups(): Flux<String>

}
