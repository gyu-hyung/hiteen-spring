package kr.jiasoft.hiteen.feature.study.dto

data class DictionaryEntry(
    val word: String,
    val phonetic: String? = null,
    val phonetics: List<DictionaryPhonetic> = emptyList()
)
