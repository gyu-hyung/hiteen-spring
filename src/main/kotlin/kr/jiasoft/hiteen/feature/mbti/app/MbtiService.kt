package kr.jiasoft.hiteen.feature.mbti.app

import kr.jiasoft.hiteen.feature.mbti.config.MbtiConfig
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswer
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service

@Service
class MbtiService(
    private val config: MbtiConfig,
) {

    fun getQuestions() = config.questions

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

        // TODO: 포인트 차감/적립 로직 추가 (예: DB에 포인트 기록 저장)

        return mapOf(
            "result" to longMbti,
            "rates" to rates,
            "title" to (result?.title ?: ""),
            "description" to (result?.description ?: "")
            // TODO: delta_point, remain_point 값 포함시키기
        )
    }



    fun viewResult(mbti: String): Map<String, Any> {
        val short = mbti.take(4)
        val result = config.results[short]
        return mapOf(
            "result" to mbti,
            "rates" to emptyList<String>(),
            "title" to (result?.title ?: ""),
            "description" to (result?.description ?: "")
        )
    }
}
