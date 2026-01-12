package kr.jiasoft.hiteen.admin.dto

import java.util.UUID
import java.time.OffsetDateTime
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonFormat

/*
 * - source: REALTIME(game_scores) / SEASON(game_rankings)
 * 관리자 랭킹 목록 조회 row
**/
data class AdminRankingResponse(

    val rank: Long,

    val source: String,

    val seasonId: Long,
    val seasonNo: String,

    val gameId: Long,
    val gameName: String,

    val league: String,

    val userId: Long,

    val userUid: UUID?,

    val nickname: String,

    val score: BigDecimal,

    val tryCount: Int? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
)