package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminGiftResponse
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminGiftRepository : CoroutineCrudRepository<GiftEntity, Long> {

    @Query("""
        SELECT
            g.id                 AS gift_id,
            g.uid                AS gift_uid,
            gu.id                AS gift_user_id,
            g.category           AS gift_category,
            g.type               AS gift_type,
            g.memo               AS memo,
            giver.nickname       AS giver_nickname,
            g.user_id            AS giver_user_id,
            receiver.nickname    AS receiver_nickname,
            gu.user_id           AS receiver_user_id,
            gu.status            AS status,
            gu.receive_date      AS receive_date,
            g.created_at         AS created_at,
            gu.coupon_no         AS coupon_no,
            gu.coupon_img        AS coupon_img,
            gu.request_date      AS request_date,
            gu.pub_date          AS pub_date,
            gu.use_date          AS use_date,
            gu.pub_expired_date  AS pub_expired_date,
            gu.use_expired_date  AS use_expired_date,
            gu.goods_code        AS goods_code,
            (SELECT goods_name FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_name,
            (SELECT goods_img_s FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_img_s,
            (SELECT goods_img_b FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_img_b,
            (SELECT brand_icon_img FROM goods_giftishow WHERE goods_code = gu.goods_code) AS brand_icon_img,
            gu.game_id           AS game_id,
            gu.season_id         AS season_id,
            gu.season_rank       AS season_rank,
            gu.point             AS point,
            gu.delivery_name     AS delivery_name,
            gu.delivery_phone    AS delivery_phone,
            gu.delivery_address1 AS delivery_address1,
            gu.delivery_address2 AS delivery_address2
        FROM gift g
        LEFT JOIN gift_users gu ON g.id = gu.gift_id
        LEFT JOIN users giver ON g.user_id = giver.id
        LEFT JOIN users receiver ON gu.user_id = receiver.id
        WHERE g.uid = :uid
        LIMIT 1
    """)
    suspend fun findByUid(uid: UUID): AdminGiftResponse?

    @Query("""
        SELECT
            g.id                 AS gift_id,
            g.uid                AS gift_uid,
            gu.id                AS gift_user_id,
            g.category           AS gift_category,
            g.type               AS gift_type,
            g.memo               AS memo,
            giver.nickname       AS giver_nickname,
            g.user_id            AS giver_user_id,
            receiver.nickname    AS receiver_nickname,
            gu.user_id           AS receiver_user_id,
            gu.status            AS status,
            gu.receive_date      AS receive_date,
            g.created_at         AS created_at,
            gu.coupon_no         AS coupon_no,
            gu.coupon_img        AS coupon_img,
            gu.request_date      AS request_date,
            gu.pub_date          AS pub_date,
            gu.use_date          AS use_date,
            gu.pub_expired_date  AS pub_expired_date,
            gu.use_expired_date  AS use_expired_date,
            gu.goods_code        AS goods_code,
            (SELECT goods_name FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_name,
            (SELECT goods_img_s FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_img_s,
            (SELECT goods_img_b FROM goods_giftishow WHERE goods_code = gu.goods_code) AS goods_img_b,
            (SELECT brand_icon_img FROM goods_giftishow WHERE goods_code = gu.goods_code) AS brand_icon_img,
            gu.game_id           AS game_id,
            gu.season_id         AS season_id,
            gu.season_rank       AS season_rank,
            gu.point             AS point,
            gu.delivery_name     AS delivery_name,
            gu.delivery_phone    AS delivery_phone,
            gu.delivery_address1 AS delivery_address1,
            gu.delivery_address2 AS delivery_address2
        FROM gift_users gu
        JOIN gift g ON g.id = gu.gift_id
        LEFT JOIN users giver ON giver.id = g.user_id
        LEFT JOIN users receiver ON receiver.id = gu.user_id
        WHERE g.deleted_at IS NULL
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        giver.nickname ILIKE '%' || :search || '%'
                        OR receiver.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'giver' THEN
                        giver.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'receiver' THEN
                        receiver.nickname ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :category IS NULL OR g.category = :category
            )
            AND (
                :type IS NULL OR g.type = :type
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'WAIT' AND gu.status = 0)
                OR (:status = 'SENT' AND gu.status = 1)
                OR (:status = 'USED' AND gu.status = 2)
                OR (:status = 'EXPIRED' AND gu.status = 3)
                OR (:status = 'DELIVERY_REQUESTED' AND gu.status = 4)
                OR (:status = 'DELIVERY_DONE' AND gu.status = 5)
                OR (:status = 'GRANT_REQUESTED' AND gu.status = 6)
                OR (:status = 'GRANTED' AND gu.status = 7)
                OR (:status = 'CANCELLED' AND gu.status = -1)
            )
            AND (
                :uid IS NULL
                OR ( receiver.uid = :uid )
            )
        ORDER BY
            CASE WHEN :order = 'DESC' THEN g.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN g.created_at END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        category: GiftCategory?,
        type: GiftType?,
    ): Flow<AdminGiftResponse>

    @Query("""
        SELECT COUNT(*)
        FROM gift_users gu
        JOIN gift g ON g.id = gu.gift_id
        LEFT JOIN users giver ON giver.id = g.user_id
        LEFT JOIN users receiver ON receiver.id = gu.user_id
        WHERE g.deleted_at IS NULL
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        giver.nickname ILIKE '%' || :search || '%'
                        OR receiver.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'giver' THEN
                        giver.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'receiver' THEN
                        receiver.nickname ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :category IS NULL OR g.category = :category
            )
            AND (
                :type IS NULL OR g.type = :type
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'WAIT' AND gu.status = 0)
                OR (:status = 'SENT' AND gu.status = 1)
                OR (:status = 'USED' AND gu.status = 2)
                OR (:status = 'EXPIRED' AND gu.status = 3)
                OR (:status = 'DELIVERY_REQUESTED' AND gu.status = 4)
                OR (:status = 'DELIVERY_DONE' AND gu.status = 5)
                OR (:status = 'GRANT_REQUESTED' AND gu.status = 6)
                OR (:status = 'GRANTED' AND gu.status = 7)
                OR (:status = 'CANCELLED' AND gu.status = -1)
            )
            AND (
                :uid IS NULL
                OR ( receiver.uid = :uid ) 
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        category: GiftCategory?,
        type: GiftType?,

    ): Int


}
