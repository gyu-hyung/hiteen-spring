package kr.jiasoft.hiteen.eloquent.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ManyToMany(
    val target: KClass<*>,            // 연결할 대상 모델
    val joinTable: String,            // 중간 테이블 이름
    val foreignKey: String,           // 현재 모델의 FK
    val relatedKey: String,           // 대상 모델의 FK
    val localKey: String = "id",      // 현재 모델 PK
    val targetKey: String = "id"      // 대상 모델 PK
)