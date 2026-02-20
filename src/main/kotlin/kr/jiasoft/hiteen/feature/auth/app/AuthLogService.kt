package kr.jiasoft.hiteen.feature.auth.app

import kr.jiasoft.hiteen.feature.auth.domain.AuthLogEntity
import kr.jiasoft.hiteen.feature.auth.infra.AuthLogRepository
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class AuthLogService(
    private val authLogRepository: AuthLogRepository
) {
    suspend fun saveLog(
        username: String?,
        eventType: String,
        tokenId: String? = null,
        clientIp: String? = null,
        userAgent: String? = null,
        details: String? = null
    ) {
        authLogRepository.save(
            AuthLogEntity(
                username = username,
                eventType = eventType,
                tokenId = tokenId,
                clientIp = clientIp,
                userAgent = userAgent,
                details = details
            )
        )
    }

    suspend fun saveLog(
        username: String?,
        eventType: String,
        token: BearerToken,
        details: String? = null
    ) {
        saveLog(
            username = username,
            eventType = eventType,
            tokenId = null, // jti extraction can be done here if needed
            clientIp = token.clientIp,
            userAgent = token.userAgent,
            details = details
        )
    }
    
    suspend fun saveLog(
        username: String?,
        eventType: String,
        exchange: ServerWebExchange,
        details: String? = null
    ) {
        val clientIp = exchange.request.remoteAddress?.address?.hostAddress
        val userAgent = exchange.request.headers.getFirst("User-Agent")
        saveLog(
            username = username,
            eventType = eventType,
            tokenId = null,
            clientIp = clientIp,
            userAgent = userAgent,
            details = details
        )
    }
}
