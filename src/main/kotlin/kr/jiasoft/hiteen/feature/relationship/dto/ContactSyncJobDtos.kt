package kr.jiasoft.hiteen.feature.relationship.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "연락처 동기화 Job 생성 응답")
data class ContactSyncJobCreateResponse(
    val jobId: String,
)

@Schema(description = "연락처 동기화 Job 상태 응답")
data class ContactSyncJobStatusResponse(
    val jobId: String,
    val status: String, // PENDING | DONE | FAILED
    val result: ContactResponse? = null,
    val errorMessage: String? = null,
)

