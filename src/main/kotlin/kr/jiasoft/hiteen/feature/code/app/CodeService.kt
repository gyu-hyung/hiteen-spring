package kr.jiasoft.hiteen.feature.code.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import kr.jiasoft.hiteen.feature.code.domain.CodeAssetEntity
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.domain.CodeStatus
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import kr.jiasoft.hiteen.feature.code.infra.CodeAssetRepository
import kr.jiasoft.hiteen.feature.code.infra.CodeRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class CodeService(
    private val codeRepository: CodeRepository,
    private val codeAssetRepository: CodeAssetRepository,
    private val assetRepository: AssetRepository,
    private val assetService: AssetService
) {
    /**
     * 파일 첨부 포함 공통 코드 생성
     */
    suspend fun createCodesWithFiles(
        group: String,
        createdUserId: Long,
        files: List<FilePart>,
        codeNamePrefix: String = ""
    ): List<CodeEntity> {
        if (files.isEmpty()) throw IllegalArgumentException("업로드할 파일이 필요합니다.")

        // 마지막 코드 번호 찾기 (그룹별 시퀀스)
        val lastCode = codeRepository.findLastCodeByGroup(group)
        var lastIndex = lastCode?.substringAfter("_")?.toIntOrNull() ?: 0

        val uploaded = assetService.uploadImages(files, createdUserId)
        val results = mutableListOf<CodeEntity>()

        uploaded.forEachIndexed { idx, asset ->
            lastIndex += 1
            val newCode = "${group.uppercase().take(1)}_%03d".format(lastIndex) // 예: E_001, C_001

            val savedCode = codeRepository.save(
                CodeEntity(
                    codeName = "$codeNamePrefix${idx + 1}",
                    code = newCode,
                    codeGroupName = group,
                    codeGroup = group.uppercase(),
                    status = CodeStatus.ACTIVE,
                    createdId = createdUserId,
                    createdAt = OffsetDateTime.now()
                )
            )

            codeAssetRepository.save(
                CodeAssetEntity(
                    codeId = savedCode.id,
                    uid = asset.uid,
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
    suspend fun listCodesByGroup(group: String): List<CodeWithAssetResponse> {
        val codes = codeRepository.findByGroup(group.uppercase()).toList()
        val results = mutableListOf<CodeWithAssetResponse>()

        for (code in codes) {
            val mapping = codeAssetRepository.findByCodeId(code.id).toList().firstOrNull()
            val url = mapping?.uid?.let { uid -> assetRepository.findByUid(uid)?.uid.toString() }

            results.add(
                CodeWithAssetResponse(
                    code = code.code,
                    name = code.codeName,
                    status = code.status ?: CodeStatus.ACTIVE,
                    url = url
                )
            )
        }
        return results
    }
}
