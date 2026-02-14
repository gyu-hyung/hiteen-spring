package kr.jiasoft.hiteen.feature.study.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.study.domain.StudyEntity
import kr.jiasoft.hiteen.feature.study.domain.StudyStatus
import kr.jiasoft.hiteen.feature.study.dto.*
import kr.jiasoft.hiteen.feature.study.infra.QuestionItemsRepository
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import kr.jiasoft.hiteen.feature.study.infra.StudyRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.time.OffsetDateTime

@Service
class StudyService(
    private val studyRepository: StudyRepository,
    private val questionItemsRepository: QuestionItemsRepository,
    private val questionRepository: QuestionRepository,
    private val expService: ExpService,
    private val mapper: ObjectMapper,

    // ✅ NFS 루트 경로 주입 (/app/assets)
    @Value("\${app.asset.storage-root}")
    private val assetStorageRoot: String,
) {

    /**
     * 영어 단어 학습 시작
     */
    suspend fun startStudy(user: UserEntity, request: StudyStartRequest): StudyStartResponse {
        val type = if (request.type == 9) 1 else request.type

                // 🔹 이미 진행 중인 학습이 있는지 검사
        val ongoing = studyRepository.findOngoingStudy(user.id, request.seasonId)

        if (ongoing != null) {
            println("✅ 기존 학습 세션 복원: uid=${ongoing.uid}")

            // 1️⃣ 기존 studyItems 에서 문제 ID 복원
            val stored = mapper.readTree(ongoing.studyItems)
            val questionIds = stored["question"].map { it.asLong() }

            // 2️⃣ 문제 아이템 및 본문 로드

            // type이 9인 경우 초등 문제로 대체

            val items = questionItemsRepository.findAllBySeasonId(request.seasonId).toList()
            val questionMap = questionRepository.findAllById(questionIds).toList().associateBy { it.id }

            // 3️⃣ 기존 학습 문제 응답 DTO 구성
            val questions = items.filter { it.questionId in questionIds }.mapNotNull { item ->
                val q = questionMap[item.questionId] ?: return@mapNotNull null
                val cleanedJson = item.answers.replace("\n", "\\n").replace("\r", "").trim()
                val options: List<String> = mapper.readValue(cleanedJson)

                StudyQuestionResponse(
                    questionId = q.id,
                    question = q.question,
                    symbol = q.symbol,
                    answer = q.answer,
                    sound = resolveSoundAsset(q.question),
                    options = options,
                    image = resolveImageAsset(q.question)
                )
            }

            return StudyStartResponse(
                uid = ongoing.uid,
                questionCount = questions.size,
                questions = questions
            )
        }

        // 🔹 새 학습 세션 생성
        val items = questionItemsRepository.findAllBySeasonIdAndType(request.seasonId, type).toList()
        if (items.isEmpty()) throw IllegalStateException("해당 시즌(${request.seasonId})에 학습 가능한 문제가 없습니다.")

        val questionIds = items.map { it.questionId }
        val questionMap = questionRepository.findAllById(questionIds).toList().associateBy { it.id }

        val questions = items.mapNotNull { item ->
            val q = questionMap[item.questionId] ?: return@mapNotNull null
            val cleanedJson = item.answers
                .replace("\n", "\\n")
                .replace("\r", "")
                .trim()

            val options: List<String> = mapper.readValue(cleanedJson)

            StudyQuestionResponse(
                questionId = q.id,
                question = q.question,
                symbol = q.symbol,
                answer = q.answer,
                sound = resolveSoundAsset(q.question),
                options = options,
                image = resolveImageAsset(q.question)
            )
        }

        // 🔹 새 StudyEntity 생성 및 저장
        val jsonItems = mapper.writeValueAsString(mapOf("question" to questionIds))
        val study = StudyEntity(
            userId = user.id,
            seasonId = request.seasonId,
            studyItems = jsonItems,
            givePoint = 0L,
            status = 1L,
            prep = 0,
            prepPoint = 0
        )
        val saved = studyRepository.save(study)

        return StudyStartResponse(
            uid = saved.uid,
            questionCount = questions.size,
            questions = questions
        )
    }

    /**
     * 학습 완료 처리
     */
    suspend fun completeStudy(userId: Long, uid: String): StudyResponse {
        val study = studyRepository.findByUidAndUserId(uid, userId)
            ?: throw IllegalArgumentException("학습 정보를 찾을 수 없습니다. uid=$uid")

        val updated = study.copy(
            status = StudyStatus.COMPLETED.code,
            completeDate = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        val saved = studyRepository.save(updated)
        val items = mapper.readValue(saved.studyItems, StudyItems::class.java)

        // 🔹 경험치 지급
        expService.grantExp(userId, "ENGLISH_STUDY", saved.id)

        return StudyResponse(
            id = saved.id,
            uid = saved.uid,
            userId = saved.userId,
            seasonId = saved.seasonId,
            questionCount = items.question.size,
            givePoint = saved.givePoint,
            status = saved.status,
            completeDate = saved.completeDate
        )
    }

    // ================================
    //  NFS 기반 Asset Resolver
    // ================================

    // 🔹 mp3 파일 경로 확인 (NFS: /app/assets/sound)
    private fun resolveSoundAsset(word: String?): String? {
        if (word.isNullOrBlank()) return null

        val safeName = word.trim().lowercase()
        val filePath = Paths.get(assetStorageRoot, "sound", "$safeName.mp3").toFile()

        // 예: /assets/sound/blossom.mp3
        return if (filePath.exists()) "/assets/sound/$safeName.mp3" else null
    }

    // 🔹 webp 파일 경로 확인 (NFS: /app/assets/word_img)
    // 지원 확장자: webp, jpg, jpeg, png, gif
    private fun resolveImageAsset(word: String?): String? {
        if (word.isNullOrBlank()) return null

        val safeName = word.trim().lowercase()
        val supportedExtensions = listOf("webp", "jpg", "jpeg", "png", "gif")

        for (ext in supportedExtensions) {
            val filePath = Paths.get(assetStorageRoot, "word_img", "$safeName.$ext").toFile()
            if (filePath.exists()) {
                return "/assets/word_img/$safeName.$ext"
            }
        }

        return null
    }
}
