package kr.jiasoft.hiteen.eloquent.relations

/** 마커 인터페이스 */
sealed interface Relation {
    val relationName: String
}

/** 1:N */
interface IOneToMany<P : Any, PID, C : Any> : Relation {
    val parentIdOf: (P) -> PID?
    val fetchChildrenByParentIds: suspend (List<PID?>) -> List<C>
    val countChildrenByParentIds: suspend (List<PID?>) -> Map<PID, Long>
    val childParentIdOf: (C) -> PID?
    val setChildrenOnParent: (P, List<C>) -> Unit
    val childClass: Class<*>?
}

/** HasOne: 부모 1 → 자식 1 */
interface IHasOne<P : Any, PID, C : Any> : Relation {
    val parentIdOf: (P) -> PID?
    /** 부모ID → 자식(nullable) */
    val fetchChildByParentIds: suspend (List<PID?>) -> Map<PID?, C?>
    val setChildOnParent: (P, C?) -> Unit
    val childClass: Class<*>?
}

/** N:1 (자식 → 부모, 흔히 belongsTo) */
interface IBelongsTo<C : Any, PID, P : Any> : Relation {
    /** 자식이 들고 있는 부모ID(FK) */
    val childParentIdOf: (C) -> PID?
    /** 부모ID들로 부모를 로드 */
    val fetchParentsByIds: suspend (List<PID?>) -> List<P>
    /** 부모 객체의 PK(ID) */
    val parentIdOfParent: (P) -> PID?
    /** 자식 객체에 부모 꽂기 */
    val setParentOnChild: (C, P?) -> Unit
    val parentClass: Class<*>?
}

/** N:M (pivot 기반) */
interface IManyToMany<P : Any, PID, C : Any> : Relation {
    val parentIdOf: (P) -> PID?
    /** 부모ID → 자식목록 */
    val fetchChildrenByParentIds: suspend (List<PID?>) -> Map<PID?, List<C>>
    val setChildrenOnParent: (P, List<C>) -> Unit
    val childClass: Class<*>?
}