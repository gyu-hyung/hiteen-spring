package kr.jiasoft.hiteen.common.helpers

/**
 * 학교 이름 관련 유틸리티
 */
object SchoolNameHelper {

    /**
     * 학교 이름 정규화 (약칭 변환)
     * - 검정고시 → "검정고시"
     * - 행정용 키워드 제거
     * - 학교급 약칭 변환 (고등학교 → 고, 중학교 → 중 등)
     */
    fun normalizeSchoolName(raw: String?): String? {
        if (raw.isNullOrBlank()) return ""

        var name = raw

        // 1️⃣ 특수 케이스: 검정고시
        if (name.contains("검정고시")) {
            return "검정고시"
        }

        // 2️⃣ 행정용 불필요 키워드 제거
        val removeKeywords = listOf(
            "학력인정",
            "병설",
            "분교장",
            "부설",
            "캠퍼스",
            "교육센터",
            "공동실습소",
            "(2년제)",
            "테크노폴리스"
        )

        removeKeywords.forEach {
            name = name?.replace(it, "")
        }

        // 3️⃣ 공백 정리
        name = name?.replace("\\s+".toRegex(), "")

        // 4️⃣ 학교급 치환 (긴 것부터!)
        val replaceMap = listOf(
            "기계공업고등학교" to "기계공고",
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

