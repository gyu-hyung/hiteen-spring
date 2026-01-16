package kr.jiasoft.hiteen.feature.board.dto

import java.util.UUID

data class BannerRow (
    val boardId: Long,
    val uid: UUID,
    val bannerType: String,
    val seq: Int,
)