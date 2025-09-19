package kr.jiasoft.hiteen.feature.school.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import java.time.LocalDate
import java.time.LocalDateTime


data class SchoolDto(

    @param:Schema(description = "학교 PK", example = "1")
    val id: Long? = null,

    @param:Schema(description = "시/도 코드", example = "11")
    val sido: String?,

    @param:Schema(description = "시/도 이름", example = "서울특별시")
    val sidoName: String?,

    @param:Schema(description = "학교 코드", example = "S12345")
    val code: String,

    @param:Schema(description = "학교 이름", example = "서울고등학교")
    val name: String,

    @param:Schema(description = "학교 유형 코드 (기본값 9)", example = "1")
    val type: Int = 9,

    @param:Schema(description = "학교 유형명", example = "고등학교")
    val typeName: String?,

    @param:Schema(description = "우편번호", example = "12345")
    val zipcode: String?,

    @param:Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    val address: String?,

    @param:Schema(description = "위도", example = "37.5665")
    val latitude: Double? = null,

    @param:Schema(description = "경도", example = "126.9780")
    val longitude: Double? = null,

    @param:Schema(description = "좌표 JSON", example = "{\"lat\":37.5665,\"lng\":126.9780}")
    val coords: String? = null,

    @JsonIgnore
    @param:Schema(hidden = true)
    val createdId: Long? = null,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    @JsonIgnore
    @param:Schema(hidden = true)
    val updatedId: Long? = null,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: LocalDateTime? = LocalDateTime.now(),

    @JsonIgnore
    @param:Schema(hidden = true)
    val deletedId: Long? = null,

    @param:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: LocalDateTime? = null,

    @param:Schema(description = "학교 설립일", example = "1995-03-01")
    val foundDate: LocalDate? = null,

    @param:Schema(description = "학교 회원 수", example = "100")
    val memberCount: Long = 0


) {
    companion object {
        fun from(entity: SchoolEntity, memberCount: Long = 0): SchoolDto =
            SchoolDto(
                id = entity.id,
                sido = entity.sido,
                sidoName = entity.sidoName,
                code = entity.code,
                name = entity.name,
                type = entity.type,
                typeName = entity.typeName,
                zipcode = entity.zipcode,
                address = entity.address,
                latitude = entity.latitude,
                longitude = entity.longitude,
                coords = entity.coords,
                createdId = entity.createdId,
                createdAt = entity.createdAt,
                updatedId = entity.updatedId,
                updatedAt = entity.updatedAt,
                deletedId = entity.deletedId,
                deletedAt = entity.deletedAt,
                foundDate = entity.foundDate,
                memberCount = memberCount
            )
    }

}
