package kr.jiasoft.hiteen.eloquent

import kr.jiasoft.hiteen.eloquent.relations.*
import java.util.concurrent.ConcurrentHashMap

class RelationRegistryImpl : RelationRegistry {

    private val store: ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, Relation>> = ConcurrentHashMap()

    override fun register(parentClass: Class<*>, relation: Relation, replace: Boolean) {
        val map = store.computeIfAbsent(parentClass) { ConcurrentHashMap() }
        if (!replace && map.containsKey(relation.relationName)) return
        map[relation.relationName] = relation
    }

    override fun get(parentClass: Class<*>, relationName: String): Relation? =
        store[parentClass]?.get(relationName)

    override fun exists(parentClass: Class<*>, relationName: String): Boolean =
        store[parentClass]?.containsKey(relationName) == true

    override fun remove(parentClass: Class<*>, relationName: String): Boolean {
        val map = store[parentClass] ?: return false
        val removed = map.remove(relationName) != null
        if (map.isEmpty()) store.remove(parentClass)
        return removed
    }

    override fun list(parentClass: Class<*>): Set<String> =
        store[parentClass]?.keys ?: emptySet()

    override fun clear() { store.clear() }

    // ----- Typed -----
    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> registerOneToMany(
        parentClass: Class<P>,
        relation: IOneToMany<P, PID, C>,
        replace: Boolean
    ) = register(parentClass, relation as Relation, replace)

    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> getOneToMany(
        parentClass: Class<P>,
        relationName: String
    ): IOneToMany<P, PID, C>? = get(parentClass, relationName) as? IOneToMany<P, PID, C>

    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> registerHasOne(
        parentClass: Class<P>,
        relation: IHasOne<P, PID, C>,
        replace: Boolean
    ) = register(parentClass, relation as Relation, replace)

    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> getHasOne(
        parentClass: Class<P>,
        relationName: String
    ): IHasOne<P, PID, C>? = get(parentClass, relationName) as? IHasOne<P, PID, C>

    @Suppress("UNCHECKED_CAST")
    override fun <C : Any, PID, P : Any> registerBelongsTo(
        childClass: Class<C>,
        relation: IBelongsTo<C, PID, P>,
        replace: Boolean
    ) = register(childClass, relation as Relation, replace)

    @Suppress("UNCHECKED_CAST")
    override fun <C : Any, PID, P : Any> getBelongsTo(
        childClass: Class<C>,
        relationName: String
    ): IBelongsTo<C, PID, P>? = get(childClass, relationName) as? IBelongsTo<C, PID, P>

    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> registerManyToMany(
        parentClass: Class<P>,
        relation: IManyToMany<P, PID, C>,
        replace: Boolean
    ) = register(parentClass, relation as Relation, replace)

    @Suppress("UNCHECKED_CAST")
    override fun <P : Any, PID, C : Any> getManyToMany(
        parentClass: Class<P>,
        relationName: String
    ): IManyToMany<P, PID, C>? = get(parentClass, relationName) as? IManyToMany<P, PID, C>
}
