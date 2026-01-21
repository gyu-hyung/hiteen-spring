package kr.jiasoft.hiteen.feature.contact.infra

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class UserContactBulkRepositoryImpl(
    private val databaseClient: DatabaseClient,
) : UserContactBulkRepository {

    override suspend fun upsertAllPhones(userId: Long, phones: List<String>) {
        if (phones.isEmpty()) return

        // r2dbc-postgresql 에서는 List<String>을 바로 배열로 바인딩하면 파라미터가 풀려버릴 수 있어서,
        // text[] 로 명시 바인딩한 뒤 unnest($2::text[]) 형태로 실행한다.
        val sql = """
            INSERT INTO user_contacts (user_id, phone)
            SELECT $1, x.phone
            FROM unnest($2::text[]) AS x(phone)
            ON CONFLICT (user_id, phone) DO UPDATE
            SET updated_at = now()
        """.trimIndent()

        databaseClient.sql(sql)
            .bind(0, userId)
            .bind(1, phones.toTypedArray())
            .fetch()
            .rowsUpdated()
            .awaitSingleOrNull()
    }
}
