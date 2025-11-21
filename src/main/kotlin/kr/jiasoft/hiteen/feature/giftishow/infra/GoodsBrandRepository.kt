package kr.jiasoft.hiteen.feature.giftishow.infra

import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsBrandEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GoodsBrandRepository : CoroutineCrudRepository<GoodsBrandEntity, Long> {

    suspend fun findByBrandCode(brandCode: String): GoodsBrandEntity?

    @Query("UPDATE goods_brand SET del_yn = 1")
    suspend fun markAllDeleted()
}
