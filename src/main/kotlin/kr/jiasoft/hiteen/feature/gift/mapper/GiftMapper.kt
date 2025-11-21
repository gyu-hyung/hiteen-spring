package kr.jiasoft.hiteen.feature.gift.mapper

import io.r2dbc.postgresql.codec.Json
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.dto.GiftRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import org.mapstruct.*

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
abstract class GiftMapper {

    private val mapper = jacksonObjectMapper()

    fun toJson(data: List<Long>): Json = Json.of(mapper.writeValueAsString(data))

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uid", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "users", expression = "java(toJson(request.getUsers()))")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    abstract fun toEntity(request: GiftRequest): GiftEntity

    @AfterMapping
    fun afterMapping(entity: GiftEntity, @MappingTarget response: GiftResponse) {}

    fun fromJson(json: Json?): List<Long> =
        json?.let { mapper.readValue(it.asString(), List::class.java).map { it.toString().toLong() } } ?: emptyList()

    @Mapping(target = "users", expression = "java(fromJson(entity.getUsers()))")
    abstract fun toResponse(entity: GiftEntity): GiftResponse
}
