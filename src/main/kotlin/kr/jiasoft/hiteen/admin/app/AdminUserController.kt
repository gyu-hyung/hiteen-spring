package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminFriendResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsGiftishowCreateRequest
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.admin.infra.AdminFollowRepository
import kr.jiasoft.hiteen.admin.infra.AdminFriendRepository
import kr.jiasoft.hiteen.admin.infra.AdminGoodsRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.play.domain.GameEntity
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.poll.dto.PollSelectResponse
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
    private val adminUserRepository: AdminUserRepository,
    private val adminFriendRepository: AdminFriendRepository,
    private val adminFollowRepository: AdminFollowRepository,
    private val adminGoodsRepository: AdminGoodsRepository,

    private val assetService: AssetService,

    //게임목록
    private val gameRepository: GameRepository,
) {


    @GetMapping("/users")
    suspend fun getUsers(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam role: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminUserResponse>>> {

        val res = adminUserRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            role = role
        ).toList()

        val totalCount = adminUserRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            role = role
        )

        return ResponseEntity.ok(ApiResult.Companion.success(PageUtil.of(res, totalCount, page, size)))
    }



    @GetMapping("/user")
    suspend fun getUserDetail(
        @RequestParam("uid") uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminUserResponse>> {

        val data = adminUserRepository.findByUid(uid)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

        val userResponse = userService.findUserResponse(data.id)

        val finalResponse = AdminUserResponse.from(data, userResponse)

        return ResponseEntity.ok(ApiResult.success(finalResponse))
    }




    @GetMapping("/friends")
    suspend fun getFriends(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: UUID? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminFriendResponse>>> {

        // 1) 목록 조회
        val list = adminFriendRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminFriendRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }



    @GetMapping("/follows")
    suspend fun getFollows(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam uid: String? = null,

        @RequestParam followType: String = "FOLLOWING",

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminFollowResponse>>> {

        val uuid = uid?.let { UUID.fromString(it) }

        val list = adminFollowRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uuid,
            followType = followType,
        ).toList()

        val totalCount = adminFollowRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uuid,
            followType = followType,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }



    private suspend fun generateNextGoodsCode(): String {
        val maxCode = adminGoodsRepository.findMaxHGoodsCode()

        val nextNumber = maxCode
            ?.substring(1)            // "00000000001"
            ?.toLongOrNull()
            ?.plus(1)
            ?: 1L                     // 최초 생성 시

        return "H" + nextNumber.toString().padStart(11, '0')
    }


    @PostMapping("/goods", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveGoods(
        @RequestPart("req") req: GoodsGiftishowCreateRequest,
        @Parameter(description = "첨부 파일 small") @RequestPart(name = "fileS", required = false) fileS: FilePart?,
        @Parameter(description = "첨부 파일 large") @RequestPart(name = "fileB", required = false) fileB: FilePart?,
        @Parameter(description = "첨부 파일 brand") @RequestPart(name = "fileBrand", required = false) fileBrand: FilePart?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>?>? {

        val sAsset =  fileS?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }
        val bAsset =  fileB?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }
        val brandAsset =  fileBrand?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }

        val res =  if (req.id == null) {

            val entity = GoodsGiftishowEntity(
                goodsNo = req.goodsNo,
                goodsCode = generateNextGoodsCode(),
                goodsName = req.goodsName,
                brandCode = req.brandCode,
                brandName = req.brandName,
                content = req.content,
                contentAddDesc = req.contentAddDesc,
                searchKeyword = req.searchKeyword,
                mdCode = req.mdCode,
                category1Seq = req.category1Seq,
                category1Name = req.category1Name,
                goodsTypeCode = req.goodsTypeCode,
                goodsTypeName = req.goodsTypeName,
                goodsTypeDetailName = req.goodsTypeDetailName,
                goodsImgS = sAsset?.uid?.toString(),
                goodsImgB = bAsset?.uid?.toString(),
                goodsDescImgWeb = req.goodsDescImgWeb,
                brandIconImg = brandAsset?.uid?.toString(),
                mmsGoodsImg = req.mmsGoodsImg,
                salePrice = req.salePrice,
                realPrice = req.realPrice,
                discountRate = req.discountRate,
                discountPrice = req.discountPrice,
                goodsComId = req.goodsComId,
                goodsComName = req.goodsComName,
                validPeriodType = req.validPeriodType,
                limitDay = req.limitDay,
                validPeriodDay = req.validPeriodDay,
                goodsStateCode = req.goodsStateCode,
                status = req.status,
            )

            adminGoodsRepository.save(entity)
        } else {
            // ✅ 수정
            val origin = adminGoodsRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${req.id}")

            val updated = origin.copy(
                goodsName = req.goodsName,
                brandCode = req.brandCode,
                brandName = req.brandName,
                content = req.content,
                contentAddDesc = req.contentAddDesc,
                searchKeyword = req.searchKeyword,
                mdCode = req.mdCode,
                category1Seq = req.category1Seq,
                category1Name = req.category1Name,
                goodsTypeCode = req.goodsTypeCode,
                goodsTypeName = req.goodsTypeName,
                goodsTypeDetailName = req.goodsTypeDetailName,
                goodsImgS = sAsset?.uid?.toString() ?: origin.goodsImgS,
                goodsImgB = bAsset?.uid?.toString() ?: origin.goodsImgB,
                goodsDescImgWeb = req.goodsDescImgWeb,
                brandIconImg = brandAsset?.uid?.toString() ?: origin.brandIconImg,
                mmsGoodsImg = req.mmsGoodsImg,
                salePrice = req.salePrice,
                realPrice = req.realPrice,
                discountRate = req.discountRate,
                discountPrice = req.discountPrice,
                goodsComId = req.goodsComId,
                goodsComName = req.goodsComName,
                validPeriodType = req.validPeriodType,
                limitDay = req.limitDay,
                validPeriodDay = req.validPeriodDay,
                goodsStateCode = req.goodsStateCode,
                status = req.status,
                updatedAt = OffsetDateTime.now(),
            )

            adminGoodsRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(res))
    }




    @GetMapping("/goods")
    suspend fun getGoods(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,

        // ⭐ 추가
        @RequestParam categorySeq: Int? = null,
        @RequestParam goodsTypeCd: String? = null,


        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<GoodsGiftishowEntity>>> {

        // 1) 목록 조회
        val list = adminGoodsRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminGoodsRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,

            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }


    @GetMapping("/goods/categories")
    suspend fun getGoodsCategories(): ResponseEntity<ApiResult<List<GoodsCategoryDto>>> {
        val list = adminGoodsRepository.findCategories().toList()
        println("list = ${list}")
        return ResponseEntity.ok(ApiResult.success(list))
    }



    @GetMapping("/goods/types")
    suspend fun getGoodsTypes(): ResponseEntity<ApiResult<List<GoodsTypeDto>>> {
        val list = adminGoodsRepository.findGoodsTypes().toList()
        println("list = ${list}")
        return ResponseEntity.ok(ApiResult.success(list))
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
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
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
        val pageData = PageUtil.of(res, res.size)

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))

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
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
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
            page = page,
            size = size
        )

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))

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
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
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
        val pageData = PageUtil.of(res, res.size, page, size)

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))
    }

    @GetMapping("/comments/vote")
    suspend fun getCommentVote(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
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
            page = page,
            size = size
        )

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))

    }


    data class GameAdminResponse (
        val no: Long,
        val id: Long,
        val title: String,
        val description: String,
        val assetUid: UUID,
        val link: String,
        val hits: Int,
        val likeCount: Int,
        val createdAt: OffsetDateTime,
    )

    @GetMapping("/games")
    suspend fun getGames(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<GameEntity>>> {

        val res = listOf(
            GameEntity(1, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(2, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(3, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(4, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(5, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(6, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(7, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(8, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(9, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
            GameEntity(10, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(11, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(12, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(13, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(14, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(15, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(16, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(17, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(18, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(19, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(20, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
//            GameEntity(21, "REACTION", "반사 신경 게임", "반사 신경을 테스트하는 게임입니다.", "ACTIVE", OffsetDateTime.now()),
        )

//        val res = gameRepository.findAll().toList()
        return ResponseEntity.ok(ApiResult.Companion.success(PageUtil.of(res, 21, page, size)))
    }

}