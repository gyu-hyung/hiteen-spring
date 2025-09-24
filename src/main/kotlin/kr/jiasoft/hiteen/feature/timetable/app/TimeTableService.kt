package kr.jiasoft.hiteen.feature.timetable.app

import kr.jiasoft.hiteen.feature.timetable.domain.TimeUserEntity
import kr.jiasoft.hiteen.feature.timetable.dto.TimeTableRequest
import kr.jiasoft.hiteen.feature.timetable.dto.TimeTableResponse
import kr.jiasoft.hiteen.feature.timetable.dto.TimeTableSlotResponse
import kr.jiasoft.hiteen.feature.timetable.infra.TimeUserRepository
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class TimeTableService(
    private val timeUserRepository: TimeUserRepository,
    private val userRepository: UserRepository
) {

    suspend fun getTimeTable(userUid: String): List<TimeTableResponse> {
        val user = userRepository.findByUid(userUid)
            ?: throw IllegalArgumentException("User not found: $userUid")

        val all = timeUserRepository.findAllByUserId(user.id).toList()
        return all.groupBy { it.week }
            .map { (week, list) ->
                TimeTableResponse(
                    week = week,
                    slots = list.sortedBy { it.period }.map {
                        TimeTableSlotResponse(
                            week = it.week,
                            period = it.period,
                            subject = it.subject
                        )
                    }
                )
            }
            .sortedBy { it.week }
    }

    suspend fun saveOrUpdate(userUid: String, request: TimeTableRequest) {
        val user = userRepository.findByUid(userUid)
            ?: throw IllegalArgumentException("User not found: $userUid")

        val existing = timeUserRepository.findByUserAndSlot(user.id, request.week, request.period)
        if (existing != null) {
            timeUserRepository.save(
                existing.copy(
                    subject = request.subject,
                    updatedAt = OffsetDateTime.now()
                )
            )
        } else {
            timeUserRepository.save(
                TimeUserEntity(
                    classId = 0,
                    userId = user.id,
                    week = request.week,
                    period = request.period,
                    subject = request.subject,
                    createdAt = OffsetDateTime.now()
                )
            )
        }
    }

    suspend fun delete(userUid: String, week: Int, period: Int) {
        val user = userRepository.findByUid(userUid)
            ?: throw IllegalArgumentException("User not found: $userUid")

        val existing = timeUserRepository.findByUserAndSlot(user.id, week, period)
        if (existing != null) {
            timeUserRepository.delete(existing)
        }
    }
}
