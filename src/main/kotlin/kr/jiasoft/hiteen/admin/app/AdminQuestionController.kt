package kr.jiasoft.hiteen.admin.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.SeasonFilterDto
import kr.jiasoft.hiteen.admin.infra.AdminQuestionRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import kr.jiasoft.hiteen.feature.study.infra.QuestionItemsRepository
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
    private val questionItemsRepository: QuestionItemsRepository,
    private val objectMapper: ObjectMapper,
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

        /* =====================
         * 4️⃣ question_items answers 업데이트
         * 정답(answer)이 변경된 경우, 해당 문제가 포함된 question_items의 answers도 업데이트
         * ===================== */
        if (origin != null && origin.answer != req.answer && req.answer != null) {
            val items = questionItemsRepository.findAllByQuestionId(saved.id).toList()
            for (item in items) {
                try {
                    // answers 문자열 파싱 (줄바꿈 등 특수문자를 이스케이프 처리)
                    val answersJson = item.answers
                        .replace("\r\n", "\\n")
                        .replace("\n", "\\n")
                        .replace("\r", "\\n")
                        .replace("\t", "\\t")

                    val answers: MutableList<String> = objectMapper.readValue(
                        answersJson,
                        object : com.fasterxml.jackson.core.type.TypeReference<MutableList<String>>() {}
                    )
                    // 기존 정답을 새 정답으로 교체
                    val oldAnswer = origin.answer
                    if (oldAnswer != null) {
                        val index = answers.indexOfFirst { it.trim() == oldAnswer.trim() }
                        if (index >= 0) {
                            answers[index] = req.answer
                            val updatedItem = item.copy(answers = objectMapper.writeValueAsString(answers))
                            questionItemsRepository.save(updatedItem)
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ question_items 업데이트 실패: itemId=${item.id}, answers=${item.answers}, error=${e.message}")
                }
            }
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

        // ⭐ 추가
        @RequestParam type: String? = null,
        @RequestParam seasonId: Long? = null,

        // ⭐ 추가: true=이미지/사운드 중 하나라도 있는 것만, false=둘 다 없는 것만
        @RequestParam hasAsset: Boolean? = null,

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
            hasAsset = hasAsset,

        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminQuestionRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,

            type = type,
            seasonId = seasonId,
            hasAsset = hasAsset,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }


    @GetMapping("/seasonFilters")
    suspend fun seasonList(
        @RequestParam status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<SeasonFilterDto?>>> {
        return ResponseEntity.ok(ApiResult.success(adminQuestionRepository.findSeasonFilters(status).toList()))
    }

    @GetMapping("/gameFilters")
    suspend fun gameList(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<SeasonFilterDto?>>> {
        return ResponseEntity.ok(ApiResult.success(adminQuestionRepository.findGameFilters().toList()))
    }



}