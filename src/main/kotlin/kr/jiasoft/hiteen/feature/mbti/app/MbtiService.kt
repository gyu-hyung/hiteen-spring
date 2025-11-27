package kr.jiasoft.hiteen.feature.mbti.app

import com.fasterxml.jackson.databind.ObjectMapper
import kr.jiasoft.hiteen.feature.mbti.config.MbtiConfig
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswer
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiResultEntity
import kr.jiasoft.hiteen.feature.mbti.infra.MbtiRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service

@Service
class MbtiService(
    private val config: MbtiConfig,
    private val objectMapper: ObjectMapper,
    private val mbtiRepository: MbtiRepository,
    private val userRepositoty: UserRepository,
) {

    fun getQuestions() = config.questions

    /** ✨ 결과 계산 후 DB에 저장하는 함수 */
    suspend fun calculateAndSave(userId: Long, answers: List<MbtiAnswer>): Map<String, Any> {
        val result = calculateResult(answers)

        val mbti = result["result"] as String
        val ratesJson = objectMapper.writeValueAsString(result["rates"])

        // 기존 기록 있으면 업데이트 (최신꺼 반영)
        val existing = mbtiRepository.findByUserId(userId)

        if (existing != null) {
            mbtiRepository.save(
                existing.copy(
                    mbti = mbti,
                    rates = ratesJson
                )
            )
        } else {
            mbtiRepository.save(
                MbtiResultEntity(
                    userId = userId,
                    mbti = mbti,
                    rates = ratesJson
                )
            )
        }

        return result
    }


    /** 기존 결과 계산 로직 (순수 계산) */
    fun calculateResult(answers: List<MbtiAnswer>): Map<String, Any> {
        val questions = config.questions.associateBy { it.index }
        val code = listOf(listOf("E","I"), listOf("N","S"), listOf("F","T"), listOf("P","J"), listOf("A","T"))

        val mbtis = mutableMapOf(0 to "E", 1 to "S", 2 to "F", 3 to "P", 4 to "T")
        val scores = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)

        for (answer in answers) {
            val q = questions[answer.number] ?: continue
            val delta = if (q.revers) answer.answer else -answer.answer
            scores[q.group] = (scores[q.group] ?: 0) + delta
        }

        val rates = mutableListOf<Map<String, Any>>()
        for ((key, score) in scores) {
            val percent = if (score != 0) {
                val idx = if (score > 0) 0 else 1
                mbtis[key] = code[key][idx]
                val r = (((score * if (score > 0) 1 else -1) + 36) / 72.0 * 100).toInt()
                if (score > 0) listOf(r, 100 - r) else listOf(100 - r, r)
            } else listOf(50, 50)

            rates.add(mapOf("type" to code[key].joinToString(""), "percent" to percent))
        }

        val frontMbti = (0..3).joinToString("") { mbtis[it]!! }
        val lastMbti = mbtis[4]!!
        val longMbti = "$frontMbti-$lastMbti"

        val result = config.results[frontMbti]

        return mapOf(
            "result" to longMbti,
            "rates" to rates,
            "title" to (result?.title ?: ""),
            "description" to (result?.description ?: "")
            // TODO: delta_point, remain_point 값 포함시키기
        )
    }



    suspend fun viewResult(userUid: String): Map<String, Any> {
        val user = userRepositoty.findByUid(userUid)?: throw IllegalArgumentException("존재하지않는 사용자")

        val saved = mbtiRepository.findByUserId(user.id)
            ?: return mapOf(
                "result" to "",
                "rates" to emptyList<String>(),
                "title" to "",
                "description" to ""
            )

        val mbti = saved.mbti
        val short = mbti.take(4)

        // config 결과 정보 매칭 (MBTI 설명)
        val resultInfo = config.results[short]

        // 저장한 text JSON → Kotlin Map List로 변환
        val rates: List<Map<String, Any>> =
            try {
                objectMapper.readValue(saved.rates, List::class.java) as List<Map<String, Any>>
            } catch (e: Exception) {
                emptyList()
            }

        return mapOf(
            "result" to mbti,
            "rates" to rates,
            "title" to (resultInfo?.title ?: ""),
            "description" to (resultInfo?.description ?: "")
        )
    }

}
