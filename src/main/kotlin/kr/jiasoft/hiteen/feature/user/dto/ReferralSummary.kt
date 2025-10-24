package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime

data class ReferralSummary(
    val user: UserSummary,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val referredAt: OffsetDateTime, // 나를 추천인으로 등록한 날짜
)
