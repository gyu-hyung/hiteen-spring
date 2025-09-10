package kr.jiasoft.hiteen.eloquent.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasMany(
    val target: KClass<*>,
    val foreignKey: String = "",
    val localKey: String = "id"
)