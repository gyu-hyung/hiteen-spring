package kr.jiasoft.hiteen.eloquent

import kr.jiasoft.hiteen.eloquent.relations.*

interface RelationRegistry {
    fun register(parentClass: Class<*>, relation: Relation, replace: Boolean = true)
    fun get(parentClass: Class<*>, relationName: String): Relation?
    fun exists(parentClass: Class<*>, relationName: String): Boolean
    fun remove(parentClass: Class<*>, relationName: String): Boolean
    fun list(parentClass: Class<*>): Set<String>
    fun clear()

    // ----- Typed helpers -----
    fun <P : Any, PID, C : Any> registerOneToMany(parentClass: Class<P>, relation: IOneToMany<P, PID, C>, replace: Boolean = true)
    fun <P : Any, PID, C : Any> getOneToMany(parentClass: Class<P>, relationName: String): IOneToMany<P, PID, C>?

    fun <P : Any, PID, C : Any> registerHasOne(parentClass: Class<P>, relation: IHasOne<P, PID, C>, replace: Boolean = true)
    fun <P : Any, PID, C : Any> getHasOne(parentClass: Class<P>, relationName: String): IHasOne<P, PID, C>?

    fun <C : Any, PID, P : Any> registerBelongsTo(childClass: Class<C>, relation: IBelongsTo<C, PID, P>, replace: Boolean = true)
    fun <C : Any, PID, P : Any> getBelongsTo(childClass: Class<C>, relationName: String): IBelongsTo<C, PID, P>?

    fun <P : Any, PID, C : Any> registerManyToMany(parentClass: Class<P>, relation: IManyToMany<P, PID, C>, replace: Boolean = true)
    fun <P : Any, PID, C : Any> getManyToMany(parentClass: Class<P>, relationName: String): IManyToMany<P, PID, C>?
}
