package kr.jiasoft.hiteen.feature.code.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.domain.CodeStatus
import kr.jiasoft.hiteen.feature.code.dto.CodeRequest
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import kr.jiasoft.hiteen.feature.code.infra.CodeRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class CodeService(
    private val codeRepository: CodeRepository,
    private val assetService: AssetService
) {
    /**
     * 파일 첨부 포함 공통 코드 생성
     */
    suspend fun createCodesWithFiles(
        group: String,
        createdUserId: Long,
        files: List<FilePart>,
        codeNamePrefix: String = "",
        col1: String? = null,
        col2: String? = null,
        col3: String? = null,
    ): List<CodeEntity> {
        if (files.isEmpty()) throw IllegalArgumentException("업로드할 파일이 필요합니다.")

        val normalizedGroup = group.uppercase()

        // 마지막 코드 번호 찾기 (EX: EMOJI_001 → 1 추출)
        val lastCode = codeRepository.findLastCodeByGroup(normalizedGroup)
        var lastIndex = lastCode?.substringAfterLast("_")?.toIntOrNull() ?: 0

        val uploaded = assetService.uploadImages(files, createdUserId, AssetCategory.CODE)
        val results = mutableListOf<CodeEntity>()

        uploaded.forEach { asset ->
            lastIndex += 1

            // 🔥 새로운 코드 규칙 적용
            val newCode = "%s_%03d".format(normalizedGroup, lastIndex)

            val savedCode = codeRepository.save(
                CodeEntity(
                    codeName = asset.originFileName,
                    code = newCode,
                    codeGroupName = group,
                    codeGroup = normalizedGroup,
                    status = CodeStatus.ACTIVE,
                    col1 = col1,
                    col2 = col2,
                    col3 = col3,
                    assetUid = asset.uid,
                    createdId = createdUserId,
                    createdAt = OffsetDateTime.now()
                )
            )

            results.add(savedCode)
        }

        return results
    }



    /**
     * 코드 그룹 단위 조회 (첨부파일 URL 포함)
     */
    suspend fun listCodesByGroup(group: String?): List<CodeWithAssetResponse> {
        return codeRepository.findByGroup(group?.uppercase()).toList()
    }


    /** 코드 단일 등록 (파일 첨부 지원) */
    suspend fun createCode(userId: Long, dto: CodeRequest, file: FilePart?): CodeWithAssetResponse {
        val uploaded = file?.let { assetService.uploadImage(it, userId, AssetCategory.CODE) }
        val entity = CodeEntity(
            codeName = dto.codeName,
            code = dto.code,
            codeGroupName = dto.groupName,
            codeGroup = dto.group.uppercase(),
            status = dto.status,
            assetUid = uploaded?.uid,
            col1 = dto.col1,
            col2 = dto.col2,
            col3 = dto.col3,
            createdId = userId,
            createdAt = OffsetDateTime.now(),

        )
        val savedCode = codeRepository.save(entity)
        return CodeWithAssetResponse.from(savedCode)
    }


    /** 코드 수정 (파일 첨부 지원, 변경된 값만 업데이트) */
    suspend fun updateCode(userId: Long, id: Long, dto: CodeRequest, file: FilePart?): CodeWithAssetResponse {
        val existing = codeRepository.findById(id)
            ?: throw IllegalArgumentException("해당 코드가 존재하지 않습니다: id=$id")

        // 새 파일이 있으면 업로드
        val uploaded = file?.let { assetService.uploadImage(it, userId, AssetCategory.CODE) }

        val updated = existing.copy(
            code = if (dto.code != existing.code) dto.code else existing.code,
            codeName = if (dto.codeName != existing.codeName) dto.codeName else existing.codeName,
            codeGroupName = if (dto.group != existing.codeGroupName) dto.group else existing.codeGroupName,
            codeGroup = if (dto.group.uppercase() != existing.codeGroup) dto.group.uppercase() else existing.codeGroup,
            status = if (dto.status != existing.status) dto.status else existing.status,
            assetUid = uploaded?.uid ?: existing.assetUid,
            col1 = if (dto.col1 != existing.col1) dto.col1 else existing.col1,
            col2 = if (dto.col2 != existing.col2) dto.col2 else existing.col2,
            col3 = if (dto.col3 != existing.col3) dto.col3 else existing.col3,
            updatedId = userId,
            updatedAt = OffsetDateTime.now()
        )

        val savedCode = codeRepository.save(updated)
        return CodeWithAssetResponse.from(savedCode)
    }


    /** 코드 삭제 (소프트 삭제 처리) */
    suspend fun deleteCode(userId: Long, id: Long) {
        val existing = codeRepository.findById(id)
            ?: throw IllegalArgumentException("해당 코드가 존재하지 않습니다: id=$id")

        val deleted = existing.copy(
            deletedId = userId,
            deletedAt = OffsetDateTime.now()
        )
        codeRepository.save(deleted)
    }


}
