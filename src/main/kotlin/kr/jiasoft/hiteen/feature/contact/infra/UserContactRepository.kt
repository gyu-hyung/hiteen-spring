package kr.jiasoft.hiteen.feature.contact.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.contact.domain.UserContactEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserContactRepository : CoroutineCrudRepository<UserContactEntity, Long> {

    fun findAllByUserId(userId: Long): Flow<UserContactEntity>

    @Query("SELECT phone FROM user_contacts WHERE user_id = :userId")
    fun findPhonesByUserId(userId: Long): Flow<String>

    @Query("""
        INSERT INTO user_contacts (user_id, phone, name)
        VALUES (:userId, :phone, :name)
        ON CONFLICT (user_id, phone) DO UPDATE
        SET updated_at = now()
        RETURNING id
    """)
    suspend fun upsert(userId: Long, phone: String, name: String? = null): Long
}
