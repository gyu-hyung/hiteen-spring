package kr.jiasoft.hiteen.admin.feature.user

import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.poll.dto.PollSelectResponse
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.min

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
) {

    data class AdminUserResponse(
        val id: Long,
        val uid: UUID,
        val assetUid: String,
        val nickname: String,
        val username: String,
        val phone: String,
        val gender: String,
        val birthday: String,
        val schoolName: String,
        val locationMode: String,
        val point: String,
        val role: String,
        val createdAt: String,
        val accessedAt: String,
        val deletedAt: String,
    )

    @GetMapping("/users")
    suspend fun getUsers(
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminUserResponse>>> {
        println("nickname = ${nickname}")
        println("email = ${email}")
        println("phone = ${phone}")
        println("status = ${status}")

        val allUsers = listOf(
            AdminUserResponse(1, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"),"131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(2, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(3, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(4, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(1, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"),"131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(2, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(3, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(4, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(1, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"),"131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(2, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(3, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(4, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(1, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"),"131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(2, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(3, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(4, UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"), "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
        )

        // 안전한 인자값 설정
        val perPage = if (sizeParam <= 0) 10 else sizeParam
        var page = if (pageParam <= 0) 1 else pageParam

        val total = allUsers.size

        // 총 페이지 수 계산 (0이면 0)
        val lastPage = if (total == 0) 0 else ((total + perPage - 1) / perPage)

        val startIndex = (page - 1) * perPage
        val endIndex = min(total, startIndex + perPage)

        val items = if (startIndex >= total || startIndex < 0) {
            emptyList()
        } else {
            allUsers.subList(startIndex, endIndex)
        }

        val result = ApiPage(total, lastPage, items, perPage, page)

        return ResponseEntity.ok(ApiResult.success(result))
    }



    @GetMapping("/users/{uid}")
    suspend fun getUserDetail(
        @PathVariable("uid") uid: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminUserResponse>> {
        println("status = ${uid}")

        val cont = AdminUserResponse(1,UUID.fromString("131e00bf-2ded-4d80-bbfe-1cca502c2f63"),"131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31")

        return ResponseEntity.ok(ApiResult.success(cont))
    }



    data class FriendAdminResponse (
        val no: Int,
        val nickname: String,
        val phone: String,
        val gender: String,
        val birthday: String,
        val schoolName: String,
        val locationMode: LocationMode,
    )

    @GetMapping("/friends")
    suspend fun getFriends(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<FriendAdminResponse>>> {

        val res = listOf(
            FriendAdminResponse(1, "hong1234", "010-9539-3637", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", LocationMode.PUBLIC),
            FriendAdminResponse(2, "hong1234", "010-9539-3637", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", LocationMode.HIDDEN),
            FriendAdminResponse(3, "hong1234", "010-9539-3637", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", LocationMode.RANDOM),
            FriendAdminResponse(4, "hong1234", "010-9539-3637", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", LocationMode.HIDDEN),
            FriendAdminResponse(5, "hong1234", "010-9539-3637", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", LocationMode.PUBLIC),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }


//    no: z.number(),
//    nickname: z.string(),
//    mbti: z.string(),
//    gender: z.string(),
//    grade: z.string(),
//    interests: z.string(),
//    status: z.string(),
    data class FollowAdminResponse (
        val no: Int,
        val nickname: String,
        val mbti: String,
        val gender: String,
        val grade: String,
        val interests: String,
        val status: FollowStatus,
    )

    @GetMapping("/follows")
    suspend fun getFollows(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<FollowAdminResponse>>> {

        val res = listOf(
            FollowAdminResponse(1, "홍길동", "ENFP", "남", "고2", "#운동, #축구, #독서", FollowStatus.ACCEPTED),
            FollowAdminResponse(2, "홍길동", "ENFP", "남", "고2", "#운동, #축구, #독서", FollowStatus.PENDING),
            FollowAdminResponse(3, "홍길동", "ENFP", "남", "고2", "#운동, #축구, #독서", FollowStatus.ACCEPTED),
            FollowAdminResponse(4, "홍길동", "ENFP", "남", "고2", "#운동, #축구, #독서", FollowStatus.PENDING),
            FollowAdminResponse(5, "홍길동", "ENFP", "남", "고2", "#운동, #축구, #독서", FollowStatus.ACCEPTED),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }



    data class BoardAdminResponse (
        val no: Int,
        val uid: String,
        val category: String,
        val subject: String,
        val content: String,
        val link: String,
        val ip: String,
        val hits: Int,
        val assetUid: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val reportCount: Int,
        val status: String,
        val address: String? = null,
        val detailAddress: String? = null,
        val lat: Double? = null,
        val lng: Double? = null,
        val createdId: Long,
        val createdAt: OffsetDateTime,
        val updatedId: Long? = null,
        val updatedAt: OffsetDateTime? = null,
        val deletedId: Long? = null,
        val deletedAt: OffsetDateTime? = null,
    )

    @GetMapping("/boards")
    suspend fun getBoards(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<BoardAdminResponse>>> {

        val res = listOf(
            BoardAdminResponse(
                1, "550e8400-e29b-41d4-a716-446655440000", "NOTICE", "제목", "내용내용내용",
                "", "", 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                LocalDate.now(), LocalDate.now(), 1, "ACTIVE", "주소", "상세주소",
                37.668942,	126.746276, 1, OffsetDateTime.now()
            ),
            BoardAdminResponse(
                2, "550e8400-e29b-41d4-a716-446655440000", "NOTICE", "제목", "내용내용내용",
                "", "", 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                LocalDate.now(), LocalDate.now(), 1, "ACTIVE", "주소", "상세주소",
                37.668942,	126.746276, 1, OffsetDateTime.now()
            ),
            BoardAdminResponse(
                3, "550e8400-e29b-41d4-a716-446655440000", "NOTICE", "제목", "내용내용내용",
                "", "", 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                LocalDate.now(), LocalDate.now(), 1, "ACTIVE", "주소", "상세주소",
                37.668942,	126.746276, 1, OffsetDateTime.now()
            ),
            BoardAdminResponse(
                4, "550e8400-e29b-41d4-a716-446655440000", "NOTICE", "제목", "내용내용내용",
                "", "", 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                LocalDate.now(), LocalDate.now(), 1, "ACTIVE", "주소", "상세주소",
                37.668942,	126.746276, 1, OffsetDateTime.now()
            ),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }



    data class PollAdminResponse (
        val no: Long,
        val id: Long,
        val question: String,
        val photos: List<String>? = emptyList(),
        val selects: List<PollSelectResponse> = emptyList(),
        val colorStart: String?,
        val colorEnd: String?,
        val voteCount: Int = 0,
        val commentCount: Int = 0,
        val reportCount: Int = 0,
        val likeCount: Int = 0,
        val allowComment: Int,
        val createdAt: OffsetDateTime,
    )

    @GetMapping("/polls")
    suspend fun getPolls(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<PollAdminResponse>>> {

        val photos = listOf("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440000")
        val selects = listOf(
            PollSelectResponse(1, 1, "김밥", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(2, 2, "피자", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(3, 3, "탕수육", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
        )

        val res = listOf(
            PollAdminResponse(1, 1, "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            PollAdminResponse(2, 1, "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            PollAdminResponse(3, 1, "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            PollAdminResponse(4, 1, "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }




    data class CommentAdminResponse (
        val no: Long,
        val id: Long,
        val uid: UUID,
        val question: String,
        val photos: List<String>? = emptyList(),
        val selects: List<PollSelectResponse> = emptyList(),
        val colorStart: String?,
        val colorEnd: String?,
        val voteCount: Int = 0,
        val commentCount: Int = 0,
        val reportCount: Int = 0,
        val likeCount: Int = 0,
        val allowComment: Int,
        val createdAt: OffsetDateTime,
    )

    @GetMapping("/comments/post")
    suspend fun getCommentPosts(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<CommentAdminResponse>>> {

        val photos = listOf("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440000")
        val selects = listOf(
            PollSelectResponse(1, 1, "김밥", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(2, 2, "피자", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(3, 3, "탕수육", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
        )

        val res = listOf(
            CommentAdminResponse(1, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(2, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(3, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(4, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }

    @GetMapping("/comments/vote")
    suspend fun getCommentVote(
        @RequestParam uid: String? = null,
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "10") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<CommentAdminResponse>>> {

        val photos = listOf("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440000")
        val selects = listOf(
            PollSelectResponse(1, 1, "김밥", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(2, 2, "피자", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(3, 3, "탕수육", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
        )

        val res = listOf(
            CommentAdminResponse(1, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(2, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(3, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(4, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = pageParam,
            size = sizeParam
        )

        return ResponseEntity.ok(ApiResult.success(pageData))

    }



}
