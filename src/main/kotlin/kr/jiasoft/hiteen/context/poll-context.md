package kr.jiasoft.hiteen.feature.poll.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.poll.domain.*
import kr.jiasoft.hiteen.feature.poll.dto.*
import kr.jiasoft.hiteen.feature.poll.infra.*
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponseIncludes
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.*

@Service
class PollService(
private val polls: PollRepository,
private val pollUsers: PollUserRepository,
private val pollPhotos: PollPhotoRepository,
private val comments: PollCommentRepository,
private val commentLikes: PollCommentLikeRepository,
private val pollLikes: PollLikeRepository,
private val pollSelects: PollSelectRepository,
private val pollSelectPhotos: PollSelectPhotoRepository,
private val friendRepository: FriendRepository,

    private val userService: UserService,
    private val assetService: AssetService,
    private val expService: ExpService,
    private val pointService: PointService,
    private val pushService: PushService,
) {

    private enum class PollStatus {
        ACTIVE,
        INACTIVE
    }



    open suspend fun listPollsByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?,
        type: String = "all",
        author: UUID?
    ): List<PollResponse> =
        polls.findSummariesByCursor(cursor, size, currentUserId, type, author)
            .map { row ->
                val user = userService.findUserResponse(row.createdId, includes = UserResponseIncludes(school = true, tier = true))

                val photos = pollPhotos.findAllByPollId(row.id)
                    .toList()
                    .sortedBy { it.seq }
                    .map { it.assetUid.toString() }

                val selects = pollSelects.findSelectResponsesByPollId(row.id)
                    .toList()

                val totalVotes = selects.sumOf { it.voteCount }

                PollResponse(
                    id = row.id,
                    question = row.question,
                    photos = photos,
                    selects = selects,
                    colorStart = row.colorStart,
                    colorEnd = row.colorEnd,
                    voteCount = totalVotes,
                    commentCount = row.commentCount,
                    likeCount = row.likeCount,
                    likedByMe = row.likedByMe,
                    votedByMe = row.votedByMe,
                    votedSeq = row.votedSeq,
                    allowComment = row.allowComment,
                    createdAt = row.createdAt,
                    user = user,
                )
            }.toList()


}



package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollSelectEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.dto.PollSelectResponse

interface PollSelectRepository : CoroutineCrudRepository<PollSelectEntity, Long> {

    fun findAllByPollId(pollId: Long): Flow<PollSelectEntity>

    @Query("""
        SELECT
            ps.id              AS id,
            ps.seq             AS seq,
            ps.content         AS content,
            ps.vote_count      AS vote_count,
            (
                SELECT psp.asset_uid
                FROM poll_select_photos psp
                WHERE psp.select_id = ps.id
                ORDER BY psp.seq ASC, psp.id ASC
                LIMIT 1
            )                  AS photos
        FROM poll_selects ps
        WHERE ps.poll_id = :pollId
        ORDER BY ps.seq ASC
    """)
    fun findSelectResponsesByPollId(
        pollId: Long
    ): Flow<PollSelectResponse>

    @Query("DELETE FROM poll_selects WHERE poll_id = :pollId")
    suspend fun deleteAllByPollId(pollId: Long)

    @Query("UPDATE poll_selects SET vote_count = vote_count + 1 WHERE id = :selectId")
    suspend fun increaseVoteCount(selectId: Long)


}




CREATE TABLE public.polls (
id bigserial NOT NULL,
question varchar(255) NULL,
photo uuid NULL,
selects jsonb NULL,
color_start varchar(20) NULL,
color_end varchar(20) NULL,
vote_count int2 DEFAULT 0 NULL,
comment_count int2 DEFAULT 0 NULL,
report_count int2 DEFAULT 0 NULL,
allow_comment int2 DEFAULT 0 NULL,
status varchar(20) NULL,
created_id int8 NULL,
created_at timestamptz DEFAULT now() NULL,
updated_at timestamptz NULL,
deleted_at timestamptz NULL,
CONSTRAINT polls_pkey PRIMARY KEY (id),
CONSTRAINT polls_photo_fkey FOREIGN KEY (photo) REFERENCES public.assets(uid)
);

CREATE TABLE public.poll_photos (
id bigserial NOT NULL,
poll_id int8 NOT NULL,
asset_uid uuid NULL,
seq int2 DEFAULT 0 NULL,
created_at timestamptz DEFAULT now() NULL,
CONSTRAINT poll_photos_pkey PRIMARY KEY (id),
CONSTRAINT poll_photos_asset_uid_fkey FOREIGN KEY (asset_uid) REFERENCES public.assets(uid),
CONSTRAINT poll_photos_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE
);


CREATE TABLE public.poll_selects (
id bigserial NOT NULL,
poll_id int8 NOT NULL,
seq int2 NOT NULL,
"content" text NULL,
vote_count int4 DEFAULT 0 NULL,
created_at timestamptz DEFAULT now() NULL,
updated_at timestamptz NULL,
CONSTRAINT poll_selects_pkey PRIMARY KEY (id),
CONSTRAINT poll_selects_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE
);



CREATE TABLE public.poll_select_photos (
id bigserial NOT NULL,
select_id int8 NOT NULL,
asset_uid uuid NULL,
seq int2 DEFAULT 0 NULL,
created_at timestamptz DEFAULT now() NULL,
CONSTRAINT poll_select_photos_pkey PRIMARY KEY (id),
CONSTRAINT poll_select_photos_asset_uid_fkey FOREIGN KEY (asset_uid) REFERENCES public.assets(uid),
CONSTRAINT poll_select_photos_select_id_fkey FOREIGN KEY (select_id) REFERENCES public.poll_selects(id) ON DELETE CASCADE
);

CREATE TABLE public.poll_likes (
id bigserial NOT NULL,
poll_id int8 NOT NULL,
user_id int8 NOT NULL,
created_at timestamptz DEFAULT now() NULL,
CONSTRAINT poll_likes_pkey PRIMARY KEY (id),
CONSTRAINT poll_likes_poll_id_user_id_key UNIQUE (poll_id, user_id),
CONSTRAINT poll_likes_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE,
CONSTRAINT poll_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);


CREATE TABLE public.poll_comments (
id bigserial NOT NULL,
poll_id int8 NOT NULL,
uid uuid DEFAULT gen_random_uuid() NULL,
parent_id int8 NULL,
"content" text NULL,
reply_count int4 DEFAULT 0 NULL,
report_count int4 DEFAULT 0 NULL,
created_id int8 NULL,
created_at timestamptz DEFAULT now() NULL,
updated_at timestamptz NULL,
deleted_at timestamptz NULL,
CONSTRAINT poll_comments_pkey PRIMARY KEY (id),
CONSTRAINT poll_comments_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.poll_comments(id) ON DELETE CASCADE,
CONSTRAINT poll_comments_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE
);

CREATE TABLE public.poll_comment_likes (
id bigserial NOT NULL,
comment_id int8 NOT NULL,
user_id int8 NOT NULL,
created_at timestamptz DEFAULT now() NULL,
CONSTRAINT poll_comment_likes_comment_id_user_id_key UNIQUE (comment_id, user_id),
CONSTRAINT poll_comment_likes_pkey PRIMARY KEY (id),
CONSTRAINT poll_comment_likes_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES public.poll_comments(id) ON DELETE CASCADE,
CONSTRAINT poll_comment_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);