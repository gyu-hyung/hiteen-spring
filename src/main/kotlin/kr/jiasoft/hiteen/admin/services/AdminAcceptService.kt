package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminAcceptFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminAcceptFriendResponse
import kr.jiasoft.hiteen.admin.infra.AdminAcceptFollowRepository
import kr.jiasoft.hiteen.admin.infra.AdminAcceptFriendRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AdminAcceptService(
    private val acceptFriendRepository: AdminAcceptFriendRepository,
    private val acceptFollowRepository: AdminAcceptFollowRepository,
) {
    // 친구 요청/승인 목록
    suspend fun friendList(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        sort: String = "DESC",
        page: Int,
        perPage: Int,
    ): ApiPage<AdminAcceptFriendResponse> {
        val page = page.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        val search = search?.trim()?.takeIf { it.isNotBlank() }

        val total = acceptFriendRepository.countBySearch(status, type, startDate, endDate, searchType, search)
        val rows = acceptFriendRepository.listBySearch(status, type, startDate, endDate, searchType, search, sort, perPage, offset).toList()

        return PageUtil.of(
            items = rows,
            total = total,
            page = page,
            size = perPage,
        )
    }

    // 팔로우 요청/승인 목록
    suspend fun followList(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        sort: String = "DESC",
        page: Int,
        perPage: Int,
    ): ApiPage<AdminAcceptFollowResponse> {
        val page = page.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        val search = search?.trim()?.takeIf { it.isNotBlank() }

        val total = acceptFollowRepository.countBySearch(status, startDate, endDate, searchType, search)
        val rows = acceptFollowRepository.listBySearch(status, startDate, endDate, searchType, search, sort, perPage, offset).toList()

        return PageUtil.of(
            items = rows,
            total = total,
            page = page,
            size = perPage,
        )
    }
}
