package kr.jiasoft.hiteen.common.context

object MetaDeltaKeys {
    const val DELTA_EXP = "meta.delta.exp"
    const val DELTA_POINT = "meta.delta.point"
    const val DELTA_CASH = "meta.delta.cash"
    const val DELTA_TIER = "meta.delta.tier"

    // ✅ 포인트 이벤트(푸시/이벤트성 호출 등)에서는 meta 응답 주입을 하지 않기 위한 플래그
    const val SKIP_META = "meta.skip"
}
