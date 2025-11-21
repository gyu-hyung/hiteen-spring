package kr.jiasoft.hiteen.feature.giftishow.infra

import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsCategoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GoodsCategoryRepository : CoroutineCrudRepository<GoodsCategoryEntity, Long> {

    suspend fun findBySeq(seq: Int): GoodsCategoryEntity?

    @Query("UPDATE goods_category SET del_yn = 1")
    suspend fun markAllDeleted()
}
