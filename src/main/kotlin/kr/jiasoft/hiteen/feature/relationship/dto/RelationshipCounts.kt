package kr.jiasoft.hiteen.feature.relationship.dto

import io.swagger.v3.oas.annotations.media.Schema


data class RelationshipCounts (

    @param:Schema(description = "게시글 수", example = "10")
    val postCount: Int = 0,
    @param:Schema(description = "투표 수", example = "10")
    val voteCount: Int = 0,
    @param:Schema(description = "틴스토리 댓글 수", example = "10")
    val boardCommentCount: Int = 0,
    @param:Schema(description = "투표 댓글 수", example = "10")
    val pollCommentCount: Int = 0,
    @param:Schema(description = "친구 수", example = "10")
    val friendCount: Int = 0,
    @param:Schema(description = "팔로워 수", example = "10")
    val followerCount: Int = 0,
    @param:Schema(description = "팔로잉 수", example = "10")
    val followingCount: Int = 0,
)
