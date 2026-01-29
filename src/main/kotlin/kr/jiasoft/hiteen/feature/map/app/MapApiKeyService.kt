package kr.jiasoft.hiteen.feature.map.app

import kr.jiasoft.hiteen.feature.map.infra.ApiKeyRepository
import org.springframework.stereotype.Service

@Service
class MapApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
) {
    suspend fun getRandomApiKeyId(type: String?): String {
        return apiKeyRepository.findRandomApiKeyId(type = type, status = "ACTIVE")
            ?: throw IllegalStateException("사용 가능한 API 키가 없습니다")
    }
}

