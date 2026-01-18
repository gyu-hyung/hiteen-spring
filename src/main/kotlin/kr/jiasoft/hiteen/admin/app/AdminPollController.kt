package kr.jiasoft.hiteen.admin.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPollDetailRow
import kr.jiasoft.hiteen.admin.dto.AdminPollResponse
import kr.jiasoft.hiteen.admin.dto.AdminPollSaveRequest
import kr.jiasoft.hiteen.admin.dto.AdminPollSelectResponse
import kr.jiasoft.hiteen.admin.infra.AdminPollRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.poll.infra.PollPhotoRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/poll")
class AdminPollController(
    private val adminPollRepository: AdminPollRepository,
    private val assetService: AssetService,
    private val pollPhotoRepository: PollPhotoRepository,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 게시글 등록 / 수정
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun save(
        @RequestPart("req") req: AdminPollSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<PollEntity>> {
//
//        val id: Long = 0,
//        val question: String,
//        val photo: UUID? = null,
//        val colorStart: String? = null,
//        val colorEnd: String? = null,
//        val voteCount: Int = 0,
//        val commentCount: Int = 0,
//        val reportCount: Int = 0,
//        val allowComment: Int = 0,
//        val status: String = "ACTIVE",//PollStatus.ACTIVE
//        val createdId: Long,
//        val createdAt: OffsetDateTime = OffsetDateTime.now(),
//        val updatedAt: OffsetDateTime? = null,
//        val deletedAt: OffsetDateTime? = null,
        val result = if (req.id == null) {
            val entity = PollEntity(
                question = req.question!!,
                colorStart = req.colorStart,
                colorEnd = req.colorEnd,
                allowComment = req.allowComment ?: 0,
                status = req.status ?: "ACTIVE",
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )

            adminPollRepository.save(entity)

        } else {
            // ✅ 수정
            val origin = adminPollRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")

            val updated = origin.copy(
                question = req.question?: origin.question,
                colorStart = req.colorStart?: origin.colorStart,
                colorEnd = req.colorEnd?: origin.colorEnd,
                allowComment = req.allowComment ?: 0,
                status = req.status?: origin.status,
                updatedAt = OffsetDateTime.now(),
            )

            adminPollRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(result))
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @DeleteMapping
    suspend fun delete(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {

        val origin = adminPollRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val deleted = origin.copy(
            deletedAt = OffsetDateTime.now(),
        )

        adminPollRepository.save(deleted)

        return ResponseEntity.ok(ApiResult.success(origin))
    }

    /**
     * 게시글 목록 조회
     */
    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminPollResponse>>> {

        val list = adminPollRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        ).toList()

        // ✅ 첨부파일(poll_photos) 조회 후 pollId별로 매핑
        val pollIds = list.map { it.id }
        val attachmentsMap: Map<Long, List<UUID>> = if (pollIds.isEmpty()) {
            emptyMap()
        } else {
            pollPhotoRepository.findAllByPollIdIn(pollIds.toTypedArray())
                .toList()
                .groupBy({ it.pollId }, { it.assetUid })
                .mapValues { (_, uids) -> uids.filterNotNull() }
        }

        val listWithAttachments = list.map { row ->
            row.copy(attachments = attachmentsMap[row.id] ?: emptyList())
        }

        val totalCount = adminPollRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(listWithAttachments, totalCount, page, size))
        )
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    suspend fun detail(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminPollResponse>> {

        val row: AdminPollDetailRow = adminPollRepository.findDetailById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        // 첨부파일 조회
        val attachments = pollPhotoRepository.findAllByPollId(id)
            .toList()
            .sortedBy { it.seq }
            .mapNotNull { it.assetUid }

        // selects(JSON) 파싱 -> List<AdminPollSelectResponse>
        val selects: List<AdminPollSelectResponse> = if (row.selects.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val node = objectMapper.readTree(row.selects)
                if (node.isArray) {
                    node.map { n ->
                        AdminPollSelectResponse(
                            id = n.get("id").asLong(),
                            seq = n.get("seq").asInt(),
                            content = if (n.hasNonNull("content")) n.get("content").asText() else null,
                            voteCount = n.get("vote_count").asInt(),
                            photo = if (n.hasNonNull("photo")) UUID.fromString(n.get("photo").asText()) else null
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val result = AdminPollResponse(
            id = row.id,
            question = row.question,
            photo = row.photo,
            voteCount = row.voteCount,
            commentCount = row.commentCount,
            likeCount = row.likeCount,
            reportCount = row.reportCount,
            allowComment = row.allowComment,
            options = row.options,
            startDate = row.startDate,
            endDate = row.endDate,
            status = row.status,
            nickname = row.nickname,
            createdAt = row.createdAt,
            attachments = attachments,
            selects = selects,
        )

        return ResponseEntity.ok(ApiResult.success(result))
    }
}
