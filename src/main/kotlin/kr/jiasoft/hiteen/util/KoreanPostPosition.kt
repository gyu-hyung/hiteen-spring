package kr.jiasoft.hiteen.util

object KoreanPostPosition {
    enum class Type(
        val consonant: String,   // 받침 있을 때
        val vowel: String        // 받침 없을 때
    ) {
        I_GA("이", "가"),
        EUL_REUL("을", "를"),
        EUN_NEUN("은", "는"),
        WA_GWA("과", "와"),
        EURO_RO("으로", "로")
    }

    fun attach(word: String, type: Type): String {
        if (word.isBlank()) return word

        val lastChar = word.last()

        // 한글이 아니면 기본적으로 모음형 사용
        if (lastChar !in '가'..'힣') {
            return word + type.vowel
        }

        val hasBatchim = (lastChar.code - '가'.code) % 28 != 0

        // (으)로 예외 처리: 받침이 ㄹ이면 "로"
        if (type == Type.EURO_RO && hasBatchim) {
            val jong = (lastChar.code - '가'.code) % 28
            if (jong == 8) { // ㄹ
                return word + "로"
            }
        }

        return word + if (hasBatchim) type.consonant else type.vowel
    }
}