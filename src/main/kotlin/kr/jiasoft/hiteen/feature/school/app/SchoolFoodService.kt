package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodAssetEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolFoodSaveRequest
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodAssetRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SchoolFoodService(
    private val schoolFoodRepository: SchoolFoodRepository,
    private val schoolFoodAssetRepository: SchoolFoodAssetRepository,
    private val assetService: AssetService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** 급식 식단 조회 */
    suspend fun getMeals(schoolId: Long, type: String?, date: LocalDate?): List<SchoolFoodEntity> {
        val now = LocalDate.now()
        val startDate: LocalDate
        val endDate: LocalDate
        val days: Int

        when (type) {
            "prev" -> {
                startDate = (date ?: now.minusDays(6)).minusDays(3)
                endDate = startDate.plusDays(3)
                days = 3
            }
            "next" -> {
                startDate = (date ?: now.plusDays(4)).plusDays(1)
                endDate = startDate.plusDays(3)
                days = 3
            }
            else -> {
                startDate = now.minusDays(3)
                endDate = startDate.plusDays(6)
                days = 7
            }
        }

        val rows = schoolFoodRepository.findBySchoolAndDateRange(
            schoolId, startDate, endDate
        ).toList()

        val result = mutableListOf<SchoolFoodEntity>()
        for (i in 0 until days) {
            val targetDate = startDate.plusDays(i.toLong())
            val found = rows.filter { it.mealDate == targetDate }
            if (found.isNotEmpty()) {
                result.addAll(found)
            } else {
                // 빈 값 채워주기
                result.add(
                    SchoolFoodEntity(
                        id = 0,
                        schoolId = schoolId,
                        mealDate = targetDate,
                        code = "2",
                        codeName = "중식",
                        meals = null,
                        calorie = null
                    )
                )
            }
        }
        return result
    }


    /** 급식 등록/수정 */
    suspend fun saveMeal(req: SchoolFoodSaveRequest) {
        schoolFoodRepository.upsert(
            schoolId = req.schoolId,
            mealDate = req.mealDate,
            code = req.code,
            codeName = req.codeName,
            meals = req.meals ?: "",
            calorie = req.calorie,
        )
        logger.info("급식 등록 완료: ${req.schoolId} ${req.mealDate} ${req.codeName}")
    }


    /** 급식 사진 저장 */
    suspend fun saveImage(
        schoolId: Long,
        userId: Long,
        year: Int,
        month: Int,
        file: FilePart?
    ): SchoolFoodAssetEntity {
        val uploadedPhoto: String? = if (file != null) {
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = userId
            )
            asset.uid.toString()
        } else null

        val entity = SchoolFoodAssetEntity(
            schoolId = schoolId,
            year = year,
            month = month,
            userId = userId,
            image = uploadedPhoto ?: "",
            status = 1,
            createdAt = LocalDateTime.now()
        )

        return schoolFoodAssetRepository.save(entity)

    }

    /** 급식 사진 조회 (이번 달 최신 1건) */
    suspend fun viewImage(schoolId: Long, year: Int, month: Int): SchoolFoodAssetEntity? {
        return schoolFoodAssetRepository.findLatest(schoolId, year, month)
    }

    /** 급식 사진 신고 TODO 신고++ */
    suspend fun reportImage(id: Long) {
        schoolFoodAssetRepository.incrementReport(id)
    }
}
