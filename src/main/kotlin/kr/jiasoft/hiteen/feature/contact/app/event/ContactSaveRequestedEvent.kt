package kr.jiasoft.hiteen.feature.contact.app.event

/**
 * 연락처 저장 요청 이벤트
 * - 비동기로 user_contacts 테이블에 저장
 */
data class ContactSaveRequestedEvent(
    val userId: Long,
    val phones: List<String>,
)

