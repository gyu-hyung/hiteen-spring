package kr.jiasoft.hiteen.feature.contact.app.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.contact.infra.UserContactBulkRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ContactEventListener(
    private val userContactBulkRepository: UserContactBulkRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    @EventListener
    fun handleContactSaveRequested(event: ContactSaveRequestedEvent) {
        scope.launch {
            try {
                val start = System.currentTimeMillis()
                userContactBulkRepository.upsertAllPhones(event.userId, event.phones)
                logger.info("[Contact] Async upsertAllPhones completed: userId=${event.userId}, count=${event.phones.size}, took=${System.currentTimeMillis() - start}ms")
            } catch (e: Exception) {
                logger.error("[Contact] Async upsertAllPhones failed: userId=${event.userId}, error=${e.message}", e)
            }
        }
    }
}

