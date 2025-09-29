package kr.jiasoft.hiteen.feature.mbti.domain

data class MbtiQuestion(
    val index: Int,
    val text: String,
    val group: Int,
    val revers: Boolean,
    val code: List<String>,
)