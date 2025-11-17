package kr.jiasoft.hiteen.feature.location.domain

import java.util.UUID

interface LocationAuthorizationService {
    /** 권한 없으면 예외 throw */
    fun assertCanSubscribe(requesterId: Long, targetUserIds: List<UUID>)
}