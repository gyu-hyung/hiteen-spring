package kr.jiasoft.hiteen.feature.map.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.map.dto.MapApiKeyResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Map", description = "지도/장소 API")
@RestController
@RequestMapping("/api/app/map")
class MapApiKeyController(
    private val service: MapApiKeyService,
) {

    @Operation(summary = "지도 API Key ID 랜덤 1개 반환")
    @GetMapping("/keys")
    suspend fun getKey(
        @RequestParam(required = false) type: String?,
    ): ResponseEntity<ApiResult<MapApiKeyResponse>> {
        val apiKeyId = service.getRandomApiKeyId(type)
        return ResponseEntity.ok(ApiResult.success(MapApiKeyResponse(apiKeyId = apiKeyId)))
    }
}

