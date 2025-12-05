package kr.jiasoft.hiteen.feature.giftishow.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftishowGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {

    suspend fun findByGoodsCode(goodsCode: String): GoodsGiftishowEntity?

    fun findAllByGoodsCodeIn(goodsCodes: List<String>): Flow<GoodsGiftishowEntity>

    @Query("UPDATE goods_giftishow SET del_yn = 1")
    suspend fun markAllDeleted()

    suspend fun findAllByOrderByCreatedAtDesc(): List<GoodsGiftishowEntity>


}
