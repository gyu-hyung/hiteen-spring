package kr.jiasoft.hiteen.eloquent.relations

data class SimpleOneToMany<P : Any, PID, C : Any>(
    override val relationName: String,
    override val parentIdOf: (P) -> PID?,
    override val fetchChildrenByParentIds: suspend (List<PID?>) -> List<C>,
    override val countChildrenByParentIds: suspend (List<PID?>) -> Map<PID, Long>,
    override val childParentIdOf: (C) -> PID?,
    override val setChildrenOnParent: (P, List<C>) -> Unit,
    override val childClass: Class<*>? = null
) : IOneToMany<P, PID, C>

data class SimpleHasOne<P : Any, PID, C : Any>(
    override val relationName: String,
    override val parentIdOf: (P) -> PID?,
    override val fetchChildByParentIds: suspend (List<PID?>) -> Map<PID?, C?>,
    override val setChildOnParent: (P, C?) -> Unit,
    override val childClass: Class<*>? = null
) : IHasOne<P, PID, C>

data class SimpleBelongsTo<C : Any, PID, P : Any>(
    override val relationName: String,
    override val childParentIdOf: (C) -> PID?,
    override val fetchParentsByIds: suspend (List<PID?>) -> List<P>,
    override val parentIdOfParent: (P) -> PID?,
    override val setParentOnChild: (C, P?) -> Unit,
    override val parentClass: Class<*>? = null
) : IBelongsTo<C, PID, P>

data class SimpleManyToMany<P : Any, PID, C : Any>(
    override val relationName: String,
    override val parentIdOf: (P) -> PID?,
    override val fetchChildrenByParentIds: suspend (List<PID?>) -> Map<PID?, List<C>>,
    override val setChildrenOnParent: (P, List<C>) -> Unit,
    override val childClass: Class<*>? = null
) : IManyToMany<P, PID, C>