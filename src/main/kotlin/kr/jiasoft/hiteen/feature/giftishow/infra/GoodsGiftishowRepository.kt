package kr.jiasoft.hiteen.feature.giftishow.infra

import kr.jiasoft.hiteen.feature.goods.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftishowGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {

    suspend fun findByGoodsCode(goodsCode: String): GoodsGiftishowEntity?

    @Query("UPDATE goods_giftishow SET del_yn = 1")
    suspend fun markAllDeleted()

}
