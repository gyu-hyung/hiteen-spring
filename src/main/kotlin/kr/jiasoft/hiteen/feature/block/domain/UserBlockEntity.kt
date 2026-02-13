package kr.jiasoft.hiteen.feature.block.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 사용자 차단 엔티티
 * - userId가 blockedUserId를 차단
 */
@Table("user_blocks")
data class UserBlockEntity(
    @Id
    val id: Long = 0,
    val userId: Long,           // 차단한 사용자
    val blockedUserId: Long,    // 차단당한 사용자
    val reason: String? = null, // 차단 사유 (선택)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

