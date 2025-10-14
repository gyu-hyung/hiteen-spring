package kr.jiasoft.hiteen.feature.study.domain

enum class StudyStatus(val code: Long) {
    PREPARE(0),   // 준비 중
    IN_PROGRESS(1), // 학습 중
    COMPLETED(2);   // 완료

    companion object {
        fun from(code: Long): StudyStatus =
            entries.find { it.code == code } ?: PREPARE
    }
}
