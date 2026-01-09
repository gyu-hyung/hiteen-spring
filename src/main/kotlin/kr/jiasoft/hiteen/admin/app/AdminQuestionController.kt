package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.SeasonFilterDto
import kr.jiasoft.hiteen.admin.infra.AdminQuestionRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/question")
class AdminQuestionController (
    private val adminQuestionRepository: AdminQuestionRepository,
    private val assetService: AssetService,
){

    private suspend fun softDeleteAssetIfNeeded(path: String, userId: Long) {
        // ex) /assets/xxxx/{uid}/view
        val uid = extractAssetUid(path) ?: return
        assetService.softDelete(uid, userId)
    }

    private fun extractAssetUid(path: String): UUID? {
        return runCatching {
            UUID.fromString(path.substringAfterLast('/').substringBefore('/'))
        }.getOrNull()
    }



    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveQuestion(
        @RequestPart("req") req: QuestionEntity,
        @RequestPart(name = "fileI", required = false) fileImage: FilePart?,
        @RequestPart(name = "fileS", required = false) fileSound: FilePart?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<QuestionEntity>> {

        val now = OffsetDateTime.now()

        val origin = if (req.id > 0) {
            adminQuestionRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 단어입니다. id=${req.id}")
        } else null

        /* =====================
         * 1️⃣ 이미지 처리
         * ===================== */
        val imagePath = when {
            fileImage != null -> {
                // 기존 이미지 삭제
                origin?.image?.let { softDeleteAssetIfNeeded(it, user.id) }
                val asset = assetService.uploadWordAsset(
                    file = fileImage,
                    word = req.question,
                    currentUserId = user.id,
                    category = AssetCategory.WORD,
                    isImage = true
                )

                asset.filePath + asset.storedFileName
            }

            // 프론트에서 이미지 제거
            origin != null && req.image == null -> {
                origin.image?.let { softDeleteAssetIfNeeded(it, user.id) }
                null
            }

            else -> origin?.image
        }

        /* =====================
         * 2️⃣ 사운드 처리
         * ===================== */
        val soundPath = when {
            fileSound != null -> {
                origin?.sound?.let { softDeleteAssetIfNeeded(it, user.id) }
                val asset = assetService.uploadWordAsset(
                    file = fileSound,
                    word = req.question,
                    currentUserId = user.id,
                    category = AssetCategory.SOUND,
                    isImage = false
                )

                asset.filePath + asset.storedFileName

            }

            origin != null && req.sound == null -> {
                origin.sound?.let { softDeleteAssetIfNeeded(it, user.id) }
                null
            }

            else -> origin?.sound
        }

        /* =====================
         * 3️⃣ Entity 저장
         * ===================== */
        val saved = if (origin == null) {
            adminQuestionRepository.save(
                req.copy(
                    image = imagePath,
                    sound = soundPath,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            adminQuestionRepository.save(
                origin.copy(
                    type = req.type,
                    category = req.category,
                    question = req.question,
                    symbol = req.symbol,
                    answer = req.answer,
                    content = req.content,
                    status = req.status,
                    image = imagePath,
                    sound = soundPath,
                    updatedAt = now,
                )
            )
        }

        return ResponseEntity.ok(ApiResult.success(saved))
    }



    @DeleteMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun deleteGoods(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>?>? {

        val origin = adminQuestionRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${id}")

        val updated = origin.copy(
            deletedAt = OffsetDateTime.now(),
        )

        adminQuestionRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success(origin))
    }


    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
//        @RequestParam id: Long? = null,
//        @RequestParam uid: String? = null,

        // ⭐ 추가
        @RequestParam type: String? = null,
        @RequestParam seasonId: Long? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<QuestionEntity?>>> {

        // 1) 목록 조회
        val list = adminQuestionRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,

            type = type,
            seasonId = seasonId,

        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminQuestionRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,

            type = type,
            seasonId = seasonId,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }


    @GetMapping("/seasonFilters")
    suspend fun seasonList(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<SeasonFilterDto?>>> {
        return ResponseEntity.ok(ApiResult.success(adminQuestionRepository.findSeasonFilters().toList()))
    }

    @GetMapping("/gameFilters")
    suspend fun gameList(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<SeasonFilterDto?>>> {
        return ResponseEntity.ok(ApiResult.success(adminQuestionRepository.findGameFilters().toList()))
    }



}