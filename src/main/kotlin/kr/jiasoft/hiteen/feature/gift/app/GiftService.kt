package kr.jiasoft.hiteen.feature.gift.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.gift.dto.GiftRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.mapper.GiftMapper
import org.springframework.stereotype.Service

@Service
class GiftService(
    private val repository: GiftRepository,
    private val mapper: GiftMapper
) {

    suspend fun createGift(request: GiftRequest): GiftResponse {
        val entity = mapper.toEntity(request)
        val saved = repository.save(entity)
        return mapper.toResponse(saved)
    }

    suspend fun getGifts(): List<GiftResponse> =
        repository.findAll().map(mapper::toResponse).toList()

}