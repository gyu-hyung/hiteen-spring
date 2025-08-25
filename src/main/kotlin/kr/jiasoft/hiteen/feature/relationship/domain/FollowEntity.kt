package kr.jiasoft.hiteen.feature.relationship.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/*
-- ========================
--팔로우
-- ========================
CREATE TABLE follows (
  id         bigserial PRIMARY KEY,
  user_id    bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  follow_id  bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  status     varchar(20),
  status_at  timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  CONSTRAINT follows_not_self CHECK (user_id <> follow_id),
  UNIQUE (user_id, follow_id)
);
 */
@Table("follows")
data class FollowEntity (

    /* 고유번호 */
    @Id
    val id: Long? = null,

    /* 사용자 ID */
    val userId: Long,

    /* 팔로우 ID */
    val followId: Long,

    /* 상태 */
    val status: String,

    /* 상태 변경일시 */
    val statusAt: OffsetDateTime? = null,

    /* 생성일시 */
    val createdAt: OffsetDateTime? = null,

    /* 수정일시 */
    val updatedAt: OffsetDateTime? = null,

    /* 삭제일시 */
    val deletedAt: OffsetDateTime? = null

)