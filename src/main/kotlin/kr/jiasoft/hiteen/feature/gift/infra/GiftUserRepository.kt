package kr.jiasoft.hiteen.feature.gift.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftRecord
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftUserRepository: CoroutineCrudRepository<GiftUsersEntity, Long> {

    suspend fun findByUserId(userId: Long): Flow<GiftUsersEntity>
    suspend fun findByUserIdOrderByIdDesc(userId: Long): List<GiftUsersEntity>
    suspend fun findByGiftIdAndUserId(giftId: Long, userId: Long): GiftUsersEntity

//    @Query("""
//        SELECT
//            g.id as gift_id,
//            g.uid as gift_uid,
//            g."type" as gift_type,
//            g.category as gift_category,
//            gu.id as gift_user_id,
//            gu.status,
//            g.memo,
//            gu.receive_date,
//            gu.coupon_no,
//            gu.coupon_img,
//            gu.request_date,
//            gu.pub_date,
//            gu.use_date,
//            gu.pub_expired_date,
//            gu.use_expired_date,
//            gu.goods_code,
//            (SELECT gg.goods_name FROM goods_giftishow gg WHERE gg.goods_code = gu.goods_code) good_name,
//            gu.game_id,
//            gu.season_id,
//            gu.season_rank,
//            gu.point,
//            gu.delivery_name,
//            gu.delivery_phone,
//            gu.delivery_address1,
//            gu.delivery_address2
//        from gift g
//        left join gift_users gu on g.id = gu.gift_id
//        where gu.user_id = :userId
//    """)
//    suspend fun findAllWithGiftUserByUserId(userId: Long) : Flow<GiftRecord>

    @Query("""
        SELECT 
            g.id as gift_id,
            g.uid as gift_uid,
            g."type" as gift_type,
            g.category as gift_category,
            gu.id as gift_user_id,
            gu.status,
            gu.user_id,
            g.memo,
            gu.receive_date,
            gu.coupon_no,
            gu.coupon_img,
            gu.request_date,
            gu.pub_date,
            gu.use_date,
            gu.pub_expired_date,
            gu.use_expired_date,
            gu.goods_code,
            (SELECT gg.goods_name FROM goods_giftishow gg WHERE gg.goods_code = gu.goods_code) good_name,
            gu.game_id,
            gu.season_id,
            gu.season_rank,
            gu.point,
            gu.delivery_name,
            gu.delivery_phone,
            gu.delivery_address1,
            gu.delivery_address2
        from gift g
        left join gift_users gu on g.id = gu.gift_id
        where gu.user_id = :userId
    """)
    suspend fun findAllWithGiftUserByUserId(userId: Long) : Flow<GiftRecord>


    @Query("""
        SELECT 
            g.id as gift_id,
            g.uid as gift_uid,
            g."type" as gift_type,
            g.category as gift_category,
            gu.id as gift_user_id,
            gu.user_id,
            gu.status,
            gu.user_id,
            g.memo,
            gu.receive_date,
            gu.coupon_no,
            gu.coupon_img,
            gu.request_date,
            gu.pub_date,
            gu.use_date,
            gu.pub_expired_date,
            gu.use_expired_date,
            gu.goods_code,
            (SELECT gg.goods_name FROM goods_giftishow gg WHERE gg.goods_code = gu.goods_code) goods_name,
            gu.game_id,
            gu.season_id,
            gu.season_rank,
            gu.point,
            gu.delivery_name,
            gu.delivery_phone,
            gu.delivery_address1,
            gu.delivery_address2
        from gift g
        left join gift_users gu on g.id = gu.gift_id
        where gu.user_id = :userId
        and gu.id = :giftUserId
    """)
    suspend fun findWithGiftUserByUserId(userId: Long, giftUserId: Long) : GiftRecord
}