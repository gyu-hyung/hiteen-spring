package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "id만 요청하고자 할 때(삭제 등)")
data class AdminIdOnlyRequest(
    val id: Long,
)
