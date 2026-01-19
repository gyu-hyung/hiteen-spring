package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.relationship.dto.ContactResponse
import kr.jiasoft.hiteen.feature.relationship.dto.ContactSyncJobStatusResponse
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ContactSyncJobService(
    private val friendService: FriendService,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class JobRecord(
        val jobId: String,
        val userId: Long,
        @Volatile var status: String, // PENDING | DONE | FAILED
        @Volatile var result: ContactResponse? = null,
        @Volatile var errorMessage: String? = null,
        val createdAt: OffsetDateTime = OffsetDateTime.now(),
    )

    private val store = ConcurrentHashMap<String, JobRecord>()

    // 간단한 메모리 TTL(프로세스 재시작 시 유실되는 light-weight 방식)
    private val ttl: Duration = Duration.ofMinutes(10)

    fun createJob(userId: Long, rawContacts: String): String {
        cleanupIfNeeded()

        val jobId = UUID.randomUUID().toString()
        val record = JobRecord(jobId = jobId, userId = userId, status = "PENDING")
        store[jobId] = record

        scope.launch {
            try {
                val result = friendService.getContactsInternal(userId = userId, rawContacts = rawContacts)
                record.result = result
                record.status = "DONE"
            } catch (e: Exception) {
                record.status = "FAILED"
                record.errorMessage = e.message ?: e.javaClass.simpleName
            }
        }

        return jobId
    }

    fun getJob(jobId: String, userId: Long): ContactSyncJobStatusResponse {
        cleanupIfNeeded()

        val record = store[jobId] ?: return ContactSyncJobStatusResponse(
            jobId = jobId,
            status = "NOT_FOUND",
            result = null,
            errorMessage = "job not found",
        )

        if (record.userId != userId) {
            // 정보 노출 방지
            return ContactSyncJobStatusResponse(
                jobId = jobId,
                status = "NOT_FOUND",
                result = null,
                errorMessage = "job not found",
            )
        }

        return ContactSyncJobStatusResponse(
            jobId = jobId,
            status = record.status,
            result = record.result,
            errorMessage = record.errorMessage,
        )
    }

    private fun cleanupIfNeeded() {
        val cutoff = OffsetDateTime.now().minus(ttl)
        store.entries.removeIf { (_, v) -> v.createdAt.isBefore(cutoff) }
    }
}

