package kr.jiasoft.hiteen.challengeRewardPolicy.domain

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("challenge_reward_policy")
data class ChallengeRewardPolicyEntity(

    @Id
    val id: Long = 0,

    /** 리워드 타입 (CASH, GIFTISHOW, GIFT_CARD 등) */
    val type: String,

    /** 리그 (BRONZE, PLATINUM, CAHLLENGER 등) */
    val league: String?,

    /** 게임 ID */
    val gameId: Long?,

    /** 지급 수량 / 지급 캐시 */
    val amount: Int?,

    /** 상품 코드 목록 (CSV or JSON) */
    val goodsCodes: String?,

    /** 지급 순위 */
    val rank: Int?,

    /** 리워드 메시지 */
    val message: String?,

    /** 관리자 메모 */
    val memo: String?,

    /** 상태 (0=비활성, 1=활성) */
    val status: Short = 1,

    /** 정렬 순서 */
    val orderNo: Int,

    /** 대표 이미지 에셋 UID */
    val assetUid: UUID?,

    /** 등록자 ID */
    val createdId: Long?,

    /** 생성일시 */
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    /** 수정자 ID */
    val updatedId: Long? = null,

    /** 수정일시 */
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    /** 삭제자 ID */
    val deletedId: Long? = null,

    /** 삭제일시 */
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null,
)