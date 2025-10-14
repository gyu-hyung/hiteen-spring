package kr.jiasoft.hiteen.feature.study.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.study.domain.StudyEntity
import kr.jiasoft.hiteen.feature.study.domain.StudyStatus
import kr.jiasoft.hiteen.feature.study.dto.StudyItems
import kr.jiasoft.hiteen.feature.study.dto.StudyQuestionResponse
import kr.jiasoft.hiteen.feature.study.dto.StudyResponse
import kr.jiasoft.hiteen.feature.study.dto.StudyStartRequest
import kr.jiasoft.hiteen.feature.study.dto.StudyStartResponse
import kr.jiasoft.hiteen.feature.study.infra.QuestionItemsRepository
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import kr.jiasoft.hiteen.feature.study.infra.StudyRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class StudyService(
    private val studyRepository: StudyRepository,
    private val questionItemsRepository: QuestionItemsRepository,
    private val questionRepository: QuestionRepository,

    private val expService: ExpService,

    private val mapper: ObjectMapper,
) {


    /**
     * 영어 단어 학습 시작
     */
    suspend fun startStudy(user: UserEntity, request: StudyStartRequest): StudyStartResponse {

        // 🔹 이미 진행 중인 학습이 있는지 검사
        val ongoing = studyRepository.findOngoingStudy(user.id, request.seasonId)

        if (ongoing != null) {
            println("✅ 기존 학습 세션 복원: uid=${ongoing.uid}")

            // 1️⃣ 기존 studyItems 에서 문제 ID 복원
            val stored = mapper.readTree(ongoing.studyItems)
            val questionIds = stored["question"].map { it.asLong() }

            // 2️⃣ 문제 아이템 및 본문 로드
            val items = questionItemsRepository.findAllBySeasonIdAndType(request.seasonId, request.type).toList()
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
                    sound = q.sound,
                    options = options,
                    image = q.content
                )
            }

            return StudyStartResponse(
                uid = ongoing.uid,
                questionCount = questions.size,
                questions = questions
            )
        }

        // 🔹 새 학습 세션 생성
        val items = questionItemsRepository.findAllBySeasonIdAndType(request.seasonId, request.type).toList()
        if (items.isEmpty()) throw IllegalStateException("해당 시즌(${request.seasonId})에 학습 가능한 문제가 없습니다.")

        val questionIds = items.map { it.questionId }
        val questionMap = questionRepository.findAllById(questionIds).toList().associateBy { it.id }

        val questions = items.mapNotNull { item ->
            val q = questionMap[item.questionId] ?: return@mapNotNull null
            val cleanedJson = item.answers.replace("\n", "\\n").replace("\r", "").trim()
            val options: List<String> = mapper.readValue(cleanedJson)

            StudyQuestionResponse(
                questionId = q.id,
                question = q.question,
                symbol = q.symbol,
                sound = q.sound,
                options = options,
                image = q.content
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
}
