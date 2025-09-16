package kr.jiasoft.hiteen.feature.code.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import kr.jiasoft.hiteen.feature.code.domain.CodeAssetEntity
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.dto.EmojiResponse
import kr.jiasoft.hiteen.feature.code.infra.CodeAssetRepository
import kr.jiasoft.hiteen.feature.code.infra.CodeRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class CodeService(
    private val codeRepository: CodeRepository,
    private val codeAssetRepository: CodeAssetRepository,
    private val assetService: AssetService,
    private val assetRepository: AssetRepository,
) {
    /** 업로드된 파일 수만큼 코드 등록 + 파일 매핑 */
    suspend fun createEmojis(
        createdUserId: Long,
        files: List<FilePart>
    ): List<CodeEntity> {
        if (files.isEmpty()) throw IllegalArgumentException("이모지 파일이 필요합니다.")

        // 1) 현재 마지막 코드 확인
        val lastCode = codeRepository.findLastCodeByGroup("EMOJI")
        var lastIndex = lastCode?.substringAfter("E_")?.toIntOrNull() ?: 0

        // 2) 파일 업로드
        val uploaded = assetService.uploadImages(files, createdUserId)

        val results = mutableListOf<CodeEntity>()

        // 3) 업로드한 파일 개수만큼 코드 + 매핑 생성
        uploaded.forEach { asset ->
            lastIndex += 1
            val newCode = "E_%03d".format(lastIndex)

            val savedCode = codeRepository.save(
                CodeEntity(
                    codeName = "", // codeName은 비워둠
                    code = newCode,
                    codeGroupName = "이모지",
                    codeGroup = "EMOJI",
                    status = "ACTIVE",
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


    /** 이모지 목록 조회 */
    suspend fun listEmojis(): List<EmojiResponse> {
        val codes = codeRepository.findByGroup("EMOJI").toList()
        val results = mutableListOf<EmojiResponse>()

        for (code in codes) {
            val mapping = codeAssetRepository.findByCodeId(code.id).toList().firstOrNull()
            val url = mapping?.uid?.let { uid ->
                assetRepository.findByUid(uid)?.uid.toString()
            }
            results.add(
                EmojiResponse(
                    code = code.code,
                    name = code.codeName,
                    status = code.status,
                    url = url
                )
            )
        }
        return results
    }


}
