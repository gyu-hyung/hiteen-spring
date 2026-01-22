package kr.jiasoft.hiteen.feature.terms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("terms")
data class TermsEntity(

    @Id
    val id: Long? = null,

    val uid: UUID = UUID.randomUUID(),                      // 약관 UID
    val category: String?,                                  // 카테고리
    val code: String?,                                       // 약관 코드
    val version: String?,                                    // 버전정보
    val title: String?,                                      // 제목
    val content: String?,                                    // 내용

    val sort: Short = 0,                                     // 정렬순서
    val isRequired: Short = 0,                               // 필수여부 (0/1)
    val status: Short = 1,                                   // 활성상태 (0/1)

    val createdId: Long? = null,                             // 등록 유저ID
    val updatedId: Long? = null,                             // 수정 유저ID
    val deletedId: Long? = null,                             // 삭제 유저ID

    val createdAt: OffsetDateTime = OffsetDateTime.now(),      // 등록일시
    val updatedAt: OffsetDateTime? = null,                    // 수정일시
    val deletedAt: OffsetDateTime? = null                     // 삭제일시
)
