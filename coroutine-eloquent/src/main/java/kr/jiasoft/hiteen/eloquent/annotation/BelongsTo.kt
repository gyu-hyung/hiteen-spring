package kr.jiasoft.hiteen.eloquent.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BelongsTo(
    val target: KClass<*>,     // 부모 엔티티 클래스
    val foreignKey: String = "", // FK 컬럼명 (비워두면 자동 추정)
    val ownerKey: String = "id"  // 부모 PK (기본값 id)
)