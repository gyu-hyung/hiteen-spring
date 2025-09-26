package kr.jiasoft.hiteen.feature.poll.app

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.r2dbc.postgresql.codec.Json
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.poll.domain.PollTemplateEntity
import kr.jiasoft.hiteen.feature.poll.dto.PollTemplateRequest
import kr.jiasoft.hiteen.feature.poll.dto.PollTemplateResponse
import kr.jiasoft.hiteen.feature.poll.infra.PollTemplateRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PollTemplateService(
    private val pollTemplates: PollTemplateRepository,
    private val objectMapper: ObjectMapper
) {
    private val mapper = jacksonObjectMapper()

    // 사용자 조회
    suspend fun listActiveTemplates(): List<PollTemplateResponse> =
        pollTemplates.findAllActive()
            .map { entity ->
                val answers: List<String> = mapper.readValue(
                    entity.answers.asString(),  // Json -> String
                    object : TypeReference<List<String>>() {}
                )


                PollTemplateResponse(
                    id = entity.id,
                    question = entity.question,
                    answers = answers,
                    state = entity.state
                )
            }
            .toList()

    suspend fun getTemplate(id: Long): PollTemplateResponse? {
        val entity = pollTemplates.findById(id) ?: return null
        if (entity.state != 1.toShort() || entity.deletedAt != null) return null

        val answers: List<String> = mapper.readValue(
            entity.answers.asString(),  // Json -> String
            object : TypeReference<List<String>>() {}
        )


        return PollTemplateResponse(
            id = entity.id,
            question = entity.question,
            answers = answers,
            state = entity.state
        )
    }


    // 관리자 전용
    suspend fun createTemplate(req: PollTemplateRequest): Long {
        val answersJson: Json = Json.of(objectMapper.writeValueAsString(req.answers))

        val save = PollTemplateEntity(
            question = req.question,
            answers = answersJson,
            state = 1, // 기본 활성화 상태 (원하시면 req.state 받아도 됨)
            createdAt = OffsetDateTime.now()
        )

        val saved = pollTemplates.save(save)
        return saved.id
    }


    suspend fun updateTemplate(id: Long, req: PollTemplateRequest) {
        val answersJson: Json = Json.of(objectMapper.writeValueAsString(req.answers))

        val existing = pollTemplates.findById(id) ?: return
        val updated = existing.copy(
            question = req.question,
            answers = answersJson,
            state = req.state,
            updatedAt = OffsetDateTime.now()
        )
        pollTemplates.save(updated)
    }


    suspend fun deleteTemplate(id: Long) {
        val existing = pollTemplates.findById(id) ?: return
        val deleted = existing.copy(deletedAt = OffsetDateTime.now())
        pollTemplates.save(deleted)
    }
}
