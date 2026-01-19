package kr.jiasoft.hiteen.feature.contact.infra

interface UserContactBulkRepository {
    suspend fun upsertAllPhones(userId: Long, phones: List<String>)
}

