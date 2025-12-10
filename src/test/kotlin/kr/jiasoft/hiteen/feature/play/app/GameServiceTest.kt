package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class GameServiceTest {

    @Autowired
    lateinit var schoolRepository: SchoolRepository


    @Test
    fun `학교 이름 정규 테스트`() = runBlocking {
        val all = schoolRepository.findAllTest()

        all.toList().forEach {
            println("${it.name} = ${normalizeSchoolName(it.name)}")
        }
    }


    fun normalizeSchoolName(raw: String?): String? {
        if (raw.isNullOrBlank()) return ""

        var name = raw

        // 1️⃣ 검정고시 특수 처리
        if (name.contains("검정고시")) {
            return "검정고시"
        }

        // 2️⃣ 불필요한 '행정 수식어'만 제거 (분교장/공동실습소는 유지!)
        val removeKeywords = listOf(
            "학력인정",
            "병설",
            "(2년제)",
            "테크노폴리스",
            "캠퍼스",
//            "교육센터"
        )

        removeKeywords.forEach {
            name = name?.replace(it, "")
        }

        // 3️⃣ 공백 정리
        name = name?.replace("\\s+".toRegex(), "")

        // 4️⃣ 학교급 축약 (긴 것부터!)
        val replaceMap = listOf(
            "기계공업고등학교" to "기계공고",
            "공업계고등학교" to "공업계고",
            "공업고등학교" to "공고",
            "고등학교" to "고",
            "중학교" to "중",
            "초등학교" to "초"
        )

        replaceMap.forEach { (from, to) ->
            name = name?.replace(from, to)
        }

        return name?.trim()
    }

}