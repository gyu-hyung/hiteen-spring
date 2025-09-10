package kr.jiasoft.hiteen.eloquent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.eloquent.annotation.BelongsTo
import kr.jiasoft.hiteen.eloquent.annotation.HasMany
import kr.jiasoft.hiteen.eloquent.annotation.HasOne
import kr.jiasoft.hiteen.eloquent.annotation.ManyToMany
import kr.jiasoft.hiteen.eloquent.dto.CursorResult
import kr.jiasoft.hiteen.eloquent.dto.O2MInfo
import kr.jiasoft.hiteen.eloquent.dto.PageResult
import kr.jiasoft.hiteen.eloquent.relations.IBelongsTo
import kr.jiasoft.hiteen.eloquent.relations.IHasOne
import kr.jiasoft.hiteen.eloquent.relations.IManyToMany
import kr.jiasoft.hiteen.eloquent.relations.IOneToMany
import kr.jiasoft.hiteen.eloquent.relations.Relation
import kr.jiasoft.hiteen.eloquent.relations.SimpleBelongsTo
import kr.jiasoft.hiteen.eloquent.relations.SimpleHasOne
import kr.jiasoft.hiteen.eloquent.relations.SimpleOneToMany
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.awaitCount
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import kotlin.math.ceil

/**
 * CoroutineCrudRepository + R2dbcEntityTemplate 기반 Eloquent 스타일 쿼리 빌더
 */
@Component
class CoroutineEloquent(
    private val template: R2dbcEntityTemplate,
    private val relations: RelationRegistry
) {

    fun <T : Any> forEntity(clazz: Class<T>): Builder<T> = Builder(clazz, template, relations)

    class Builder<T : Any>(
        private val entityClass: Class<T>,
        private val template: R2dbcEntityTemplate,
        private val relations: RelationRegistry
    ) {
        private val criteria = mutableListOf<Criteria>()

        private var limitSize: Int? = null
        private var offsetSize: Long? = null
        private val sortOrders = mutableListOf<Sort.Order>()

        // 점 표기 경로 저장
        private val withRelationPaths = mutableListOf<List<String>>()
        private val withCountPaths = mutableListOf<List<String>>()

        private val mapper = jacksonObjectMapper().registerKotlinModule()

        // Builder<T> 내부에 추가
        private fun buildSingleCriteria(field: String, op: String, value: Any): Criteria {
            return when (op.lowercase()) {
                "=", "=="    -> Criteria.where(field).`is`(value)
                "!=", "<>"   -> Criteria.where(field).not(value)
                ">"          -> Criteria.where(field).greaterThan(value)
                ">="         -> Criteria.where(field).greaterThanOrEquals(value)
                "<"          -> Criteria.where(field).lessThan(value)
                "<="         -> Criteria.where(field).lessThanOrEquals(value)
                "like"       -> Criteria.where(field).like(value.toString())
                else         -> error("Unsupported operator: $op")
            }
        }
        fun where(field: String, op: String, value: Any): Builder<T> {
            criteria += buildSingleCriteria(field, op, value)
            return this
        }
        fun whereIn(field: String, values: Collection<Any?>): Builder<T> {
            criteria += Criteria.where(field).`in`(values)
            return this
        }
        fun whereNotIn(field: String, values: Collection<Any?>): Builder<T> {
            criteria += Criteria.where(field).notIn(values)
            return this
        }
        fun whereNull(field: String): Builder<T> {
            criteria += Criteria.where(field).isNull
            return this
        }
        fun whereNotNull(field: String): Builder<T> {
            criteria += Criteria.where(field).isNotNull
            return this
        }
        fun whereBetween(field: String, start: Any, end: Any): Builder<T> {
            val c = Criteria.where(field).greaterThanOrEquals(start)
                .and(Criteria.where(field).lessThanOrEquals(end))
            criteria += c
            return this
        }
        fun whereNotBetween(field: String, start: Any, end: Any): Builder<T> {
            val left  = Criteria.where(field).lessThan(start)
            val right = Criteria.where(field).greaterThan(end)
            criteria += left.or(right)
            return this
        }
        fun whereLike(field: String, pattern: String): Builder<T> {
            criteria += Criteria.where(field).like(pattern)
            return this
        }
        fun whereStartsWith(field: String, prefix: String): Builder<T> {
            criteria += Criteria.where(field).like("$prefix%")
            return this
        }
        fun whereEndsWith(field: String, suffix: String): Builder<T> {
            criteria += Criteria.where(field).like("%$suffix")
            return this
        }
        fun whereContains(field: String, needle: String): Builder<T> {
            criteria += Criteria.where(field).like("%$needle%")
            return this
        }
        // Builder<T> 내부에 추가

        /** OR 그룹: 내부 조건들을 OR로 묶어서 상위 AND 체인에 추가 */
        fun whereAny(block: Group.() -> Unit): Builder<T> {
            val g = Group().apply(block)
            val c = g.criteriaList.reduceOrNull(Criteria::or) ?: Criteria.empty()
            criteria += c
            return this
        }

        /** AND 그룹: 내부 조건들을 AND로 묶어서 상위 AND 체인에 추가 */
        fun whereAll(block: Group.() -> Unit): Builder<T> {
            val g = Group().apply(block)
            val c = g.criteriaList.reduceOrNull(Criteria::and) ?: Criteria.empty()
            criteria += c
            return this
        }

        /** OR 한 건만 필요한 경우: whereAny { where(...) } 형태로 사용 */
        fun orWhere(field: String, op: String, value: Any): Builder<T> =
            whereAny { where(field, op, value) }

        /** 내부 그룹 빌더 */
        inner class Group internal constructor() {
            internal val criteriaList = mutableListOf<Criteria>()

            fun where(field: String, op: String, value: Any) {
                criteriaList += buildSingleCriteria(field, op, value)
            }
            fun whereIn(field: String, values: Collection<Any?>) {
                criteriaList += Criteria.where(field).`in`(values)
            }
            fun whereNotIn(field: String, values: Collection<Any?>) {
                criteriaList += Criteria.where(field).notIn(values)
            }
            fun whereNull(field: String) {
                criteriaList += Criteria.where(field).isNull
            }
            fun whereNotNull(field: String) {
                criteriaList += Criteria.where(field).isNotNull
            }
            fun whereBetween(field: String, start: Any, end: Any) {
                val c = Criteria.where(field).greaterThanOrEquals(start)
                    .and(Criteria.where(field).lessThanOrEquals(end))
                criteriaList += c
            }
            fun whereNotBetween(field: String, start: Any, end: Any) {
                // (field < start) OR (field > end)
                val left  = Criteria.where(field).lessThan(start)
                val right = Criteria.where(field).greaterThan(end)
                criteriaList += left.or(right)
            }
            fun whereLike(field: String, pattern: String) {
                criteriaList += Criteria.where(field).like(pattern)
            }
            fun whereStartsWith(field: String, prefix: String) {
                criteriaList += Criteria.where(field).like("$prefix%")
            }
            fun whereEndsWith(field: String, suffix: String) {
                criteriaList += Criteria.where(field).like("%$suffix")
            }
            fun whereContains(field: String, needle: String) {
                criteriaList += Criteria.where(field).like("%$needle%")
            }
        }
        fun orderBy(field: String, direction: String = "asc"): Builder<T> {
            sortOrders += if (direction.equals("desc", true)) Sort.Order.desc(field) else Sort.Order.asc(field)
            return this
        }

        /** 라라벨 스타일 with: "points", "points.messages" 지원 */
        fun with(vararg relationNames: String): Builder<T> {
            relationNames.forEach { full ->
                val path = parsePath(full)
                ensurePathRegistered(entityClass, path)
                withRelationPaths += path
            }
            return this
        }

        /**
         * withCount:
         * - 1단계: withCount("points")  -> 루트(User)에 pointsCount
         * - 2단계+: withCount("points.messages") -> 각 Point에 messagesCount  (요구사항)
         */
        fun withCount(vararg relationNames: String): Builder<T> {
            relationNames.forEach { full ->
                val path = parsePath(full)
                ensurePathRegistered(entityClass, path)
                // 2단계 이상이면 부모 단계가 메모리에 로드되도록 with 추가
                if (path.size >= 2) {
                    val parentPath = path.dropLast(1)
                    if (withRelationPaths.none { it == parentPath }) withRelationPaths += parentPath
                }
                withCountPaths += path
            }
            return this
        }

        /** 전체 조회 */
        suspend fun get(): List<T> {
            val parents = template.select(entityClass).matching(buildQuery()).flow().toList()
            if (parents.isEmpty()) return emptyList()
            applyEagerLoadsAndCounts(parents)
            return parents
        }

        /** 첫 레코드 */
        suspend fun first(): T? {
            val list = template.select(entityClass).matching(orderByFirstFallback().limit(1)).flow().toList()
            val first = list.firstOrNull() ?: return null
            applyEagerLoadsAndCounts(list)
            return first
        }

        /** 페이지네이션 */
        suspend fun paginate(page: Int, perPage: Int): PageResult<T> {
            val q = buildQuery()
            val total = template.select(entityClass).matching(q).awaitCount()
            val data = template.select(entityClass)
                .matching(q.limit(perPage).offset(((page - 1) * perPage).toLong()))
                .flow().toList()
            applyEagerLoadsAndCounts(data)
            val last = ceil(total.toDouble() / perPage).toLong()
            return PageResult(data, total, perPage, page, last)
        }

        /** 커서 페이지네이션 */
        suspend fun <ID : Comparable<ID>> cursorPaginate(perPage: Int, after: ID? = null, idField: String = "id"): CursorResult<T, ID> {
            if (after != null) criteria += Criteria.where(idField).greaterThan(after)
            val data = template.select(entityClass)
                .matching(buildQuery().limit(perPage + 1))
                .flow().toList()
            val hasMore = data.size > perPage
            val page = if (hasMore) data.take(perPage) else data
            @Suppress("UNCHECKED_CAST")
            val next = page.lastOrNull()?.let { e ->
                val f = e::class.java.getDeclaredField(idField).apply { isAccessible = true }
                f.get(e) as? ID
            }
            applyEagerLoadsAndCounts(page)
            return CursorResult(page, next, hasMore)
        }

        fun toJson(list: List<T>): String = mapper.writeValueAsString(list)
        fun toJson(entity: T?): String = mapper.writeValueAsString(entity)

        /** =================== 내부 유틸 =================== */

        // 메서드 추가
        fun take(n: Int): Builder<T> { limitSize = n; return this }
        fun skip(n: Long): Builder<T> { offsetSize = n; return this }

        private fun buildQuery(): Query {
            val c = criteria.reduceOrNull(Criteria::and) ?: Criteria.empty()
            val s = if (sortOrders.isEmpty()) Sort.unsorted() else Sort.by(sortOrders)
            var q = Query.query(c).sort(s)
            limitSize?.let { q = q.limit(it) }
            offsetSize?.let { q = q.offset(it) }
            return q
        }

        private fun orderByFirstFallback(): Query {
            val q = buildQuery()
            return if (sortOrders.isEmpty()) q.sort(Sort.by(Sort.Order.asc("id"))) else q
        }

        /** with / withCount 공통 처리 */
        private suspend fun applyEagerLoadsAndCounts(rootParents: List<T>) {
            if (rootParents.isEmpty()) return
            // 1) with: 경로별 로딩
            withRelationPaths.forEach { path ->
                eagerLoadPath(entityClass, rootParents as List<Any>, path)
            }
            // 2) withCount
            withCountPaths.forEach { path ->
                if (path.size == 1) {
                    // 루트에 *Count
                    loadCountAggregateToRoot(entityClass, rootParents as List<Any>, path.first())
                } else {
                    // 부모 레벨 각 객체에 lastNameCount
                    loadCountToImmediateParents(entityClass, rootParents as List<Any>, path)
                }
            }
        }

        /** 경로 기반 eager load */
        private suspend fun eagerLoadPath(rootClass: Class<*>, rootParents: List<Any>, path: List<String>) {
            var currentClass: Class<*> = rootClass
            var currentParents: List<Any> = rootParents

            @Suppress("UNCHECKED_CAST")
            path.forEach { name ->
                when (val rel = getRequiredRelation(currentClass, name)) {
                    is IOneToMany<*, *, *> -> {
                        val r = rel as IOneToMany<Any, Any?, Any>
                        val parentIds = currentParents.mapNotNull { r.parentIdOf(it) }.distinct()
                        if (parentIds.isEmpty()) return
                        val children = r.fetchChildrenByParentIds(parentIds)
                        val groups = children.groupBy { r.childParentIdOf(it) }
                        currentParents.forEach { p ->
                            r.setChildrenOnParent(p, groups[r.parentIdOf(p)] ?: emptyList())
                        }
                        if (children.isEmpty()) return
                        currentParents = children
                        currentClass = children.first().javaClass
                    }
                    is IManyToMany<*, *, *> -> {
                        val r = rel as IManyToMany<Any, Any?, Any>
                        val parentIds = currentParents.mapNotNull { r.parentIdOf(it) }.distinct()
                        if (parentIds.isEmpty()) return
                        val map = r.fetchChildrenByParentIds(parentIds)
                        currentParents.forEach { p -> r.setChildrenOnParent(p, map[r.parentIdOf(p)] ?: emptyList()) }
                        val allChildren = map.values.flatten()
                        if (allChildren.isEmpty()) return
                        currentParents = allChildren
                        currentClass = allChildren.first().javaClass
                    }
                    is IHasOne<*, *, *> -> {
                        val r = rel as IHasOne<Any, Any?, Any>
                        val parentIds = currentParents.mapNotNull { r.parentIdOf(it) }.distinct()
                        if (parentIds.isEmpty()) return
                        val map = r.fetchChildByParentIds(parentIds)  // Map<PID, C?>
                        currentParents.forEach { p -> r.setChildOnParent(p, map[r.parentIdOf(p)]) }
                        val childs = map.values.filterNotNull()
                        if (childs.isEmpty()) return
                        currentParents = childs
                        currentClass = childs.first().javaClass
                    }
                    is IBelongsTo<*, *, *> -> {
                        val r = rel as IBelongsTo<Any, Any?, Any>
                        val fkIds = currentParents.mapNotNull { r.childParentIdOf(it) }.distinct()
                        if (fkIds.isEmpty()) return
                        val parentsLoaded = r.fetchParentsByIds(fkIds)
                        val byId = parentsLoaded.associateBy { r.parentIdOfParent(it) }
                        currentParents.forEach { c -> r.setParentOnChild(c, byId[r.childParentIdOf(c)]) }
                        val uniqParents = parentsLoaded.distinctBy { r.parentIdOfParent(it) }
                        if (uniqParents.isEmpty()) return
                        currentParents = uniqParents
                        currentClass = uniqParents.first().javaClass
                    }
                }
            }
        }

        /** 1단계 count → 루트에 주입 (ex: pointsCount) */
        private suspend fun loadCountAggregateToRoot(rootClass: Class<*>, rootParents: List<Any>, lastName: String) {
            val rel = getRequiredRelation(rootClass, lastName)
            val fieldName = "${lastName}Count"
            val idField = resolveIdFieldName(rootClass)

            @Suppress("UNCHECKED_CAST")
            when (rel) {
                is IOneToMany<*, *, *> -> {
                    val r = rel as IOneToMany<Any, Any?, Any>
                    val ids = rootParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val counts = r.countChildrenByParentIds(ids)
                    rootParents.forEach { p ->
                        val id = getFieldValue(p, idField)
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, counts[id] ?: 0L) }
                    }
                }
                is IManyToMany<*, *, *> -> {
                    val r = rel as IManyToMany<Any, Any?, Any>
                    val ids = rootParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val map = r.fetchChildrenByParentIds(ids)
                    rootParents.forEach { p ->
                        val c = (map[r.parentIdOf(p)]?.size ?: 0).toLong()
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, c) }
                    }
                }
                is IHasOne<*, *, *> -> {
                    val r = rel as IHasOne<Any, Any?, Any>
                    val ids = rootParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val map = r.fetchChildByParentIds(ids)
                    rootParents.forEach { p ->
                        val exists = if (map[r.parentIdOf(p)] != null) 1L else 0L
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, exists) }
                    }
                }
                is IBelongsTo<*, *, *> -> {
                    val r = rel as IBelongsTo<Any, Any?, Any>
                    val fkIds = rootParents.mapNotNull { r.childParentIdOf(it) }.distinct()
                    val parentsLoaded = if (fkIds.isEmpty()) emptyList() else r.fetchParentsByIds(fkIds)
                    val existSet = parentsLoaded.mapNotNull { r.parentIdOfParent(it) }.toHashSet()
                    rootParents.forEach { c ->
                        val exists = if (r.childParentIdOf(c)?.let { existSet.contains(it) } == true) 1L else 0L
                        runCatching { c::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(c, exists) }
                    }
                }
            }
        }

        /** 2단계+ count → 즉시 부모(마지막 이전 경로)의 각 객체에 lastNameCount 주입 (ex: Point.messagesCount) */
        private suspend fun loadCountToImmediateParents(rootClass: Class<*>, rootParents: List<Any>, path: List<String>) {
            val parentPath = path.dropLast(1)
            val lastName = path.last()

            eagerLoadPath(rootClass, rootParents, parentPath)

            val (parentClass, immediateParents) = getEntitiesAtPath(rootClass, rootParents, parentPath)
            if (immediateParents.isEmpty()) return

            val rel = getRequiredRelation(parentClass, lastName)
            val fieldName = "${lastName}Count"

            when (rel) {
                is IOneToMany<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST") val r = rel as IOneToMany<Any, Any?, Any>
                    val ids = immediateParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val children = r.fetchChildrenByParentIds(ids)
                    val countMap = children.groupBy { r.childParentIdOf(it) }.mapValues { it.value.size.toLong() }
                    immediateParents.forEach { p ->
                        val c = countMap[r.parentIdOf(p)] ?: 0L
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, c) }
                    }
                }
                is IManyToMany<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST") val r = rel as IManyToMany<Any, Any?, Any>
                    val ids = immediateParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val map = r.fetchChildrenByParentIds(ids)
                    immediateParents.forEach { p ->
                        val c = (map[r.parentIdOf(p)]?.size ?: 0).toLong()
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, c) }
                    }
                }
                is IHasOne<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST") val r = rel as IHasOne<Any, Any?, Any>
                    val ids = immediateParents.mapNotNull { r.parentIdOf(it) }.distinct()
                    if (ids.isEmpty()) return
                    val map = r.fetchChildByParentIds(ids)
                    immediateParents.forEach { p ->
                        val exists = if (map[r.parentIdOf(p)] != null) 1L else 0L
                        runCatching { p::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(p, exists) }
                    }
                }
                is IBelongsTo<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST") val r = rel as IBelongsTo<Any, Any?, Any>
                    val fkIds = immediateParents.mapNotNull { r.childParentIdOf(it) }.distinct()
                    val parentsLoaded = if (fkIds.isEmpty()) emptyList() else r.fetchParentsByIds(fkIds)
                    val existSet = parentsLoaded.mapNotNull { r.parentIdOfParent(it) }.toHashSet()
                    immediateParents.forEach { c ->
                        val exists = if (r.childParentIdOf(c)?.let { existSet.contains(it) } == true) 1L else 0L
                        runCatching { c::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.set(c, exists) }
                    }
                }
                else -> error("Unsupported relation type for '$lastName' on ${parentClass.simpleName}")
            }
        }

        /** 루트에서 경로로 내려가며 최종 부모 단계 객체들과 그 클래스 반환 */
        private fun getEntitiesAtPath(rootClass: Class<*>, rootParentsIn: List<Any>, path: List<String>): Pair<Class<*>, List<Any>> {
            var currentClass: Class<*> = rootClass
            var currentParents: List<Any> = rootParentsIn
            path.forEach { name ->
                // 1) 컬렉션 필드 시도
                val coll = findCollectionFieldByCandidates(currentClass, name)
                if (coll != null) {
                    val children = currentParents.flatMap { p ->
                        @Suppress("UNCHECKED_CAST")
                        (coll.get(p) as? Collection<Any>)?.toList() ?: emptyList()
                    }
                    currentClass = if (children.isNotEmpty()) children.first().javaClass
                    else resolveChildClassByConvention(currentClass, name)
                        ?: error("Cannot resolve child class for '${currentClass.simpleName}.$name'")
                    currentParents = children
                    return@forEach
                }
                // 2) 단일 필드 시도 (HasOne/BelongsTo)
                val single = getFieldOrNull(currentClass, name)
                    ?: error("Field '${currentClass.simpleName}.$name' not found. Add field or register relation.")
                val children = currentParents.mapNotNull { p -> single.get(p) }
                currentClass = if (children.isNotEmpty()) children.first().javaClass else single.type
                currentParents = children
            }
            return currentClass to currentParents
        }

        /** ======= 레지스트리/리플렉션 유틸 ======= */

        private fun getRequiredRelation(parentClass: Class<*>, name: String): Relation =
            relations.get(parentClass, name)
                ?: error("Relation '$name' is not registered for ${parentClass.simpleName}")

        private fun parsePath(full: String): List<String> =
            full.split('.').filter { it.isNotBlank() }

        private fun ensurePathRegistered(rootClass: Class<*>, path: List<String>) {
            var current = rootClass
            path.forEach { name ->
                val exists = relations.get(current, name)
                current = if (exists != null) {
                    resolveChildClassByConvention(current, name) ?: current
                } else {
                    autoRegisterByConvention(current, name) // 등록하며 childClass 반환
                }
            }
        }

        private fun resolveIdFieldName(clazz: Class<*>): String {
            val simple = clazz.simpleName
            return when {
                getFieldOrNull(clazz, "id") != null -> "id"
                getFieldOrNull(clazz, "${lowerCamel(simple)}Id") != null -> "${lowerCamel(simple)}Id"
                else -> error("Cannot find id field on ${clazz.simpleName}. Name it 'id' or register relation manually.")
            }
        }

        private fun getFieldValue(target: Any, fieldName: String): Any? =
            target::class.java.getDeclaredField(fieldName).apply { isAccessible = true }.get(target)

        private fun lowerCamel(name: String): String = name.replaceFirstChar { it.lowercase() }

        private fun getFieldOrNull(clazz: Class<*>, name: String) =
            runCatching { clazz.getDeclaredField(name) }.getOrNull()

        private fun findCollectionFieldByCandidates(parentClass: Class<*>, base: String): java.lang.reflect.Field? {
            val candidates = listOf(base, "${base}s", "${base}List")
            for (n in candidates) {
                val f = getFieldOrNull(parentClass, n)
                if (f != null && Collection::class.java.isAssignableFrom(f.type)) {
                    f.isAccessible = true
                    return f
                }
            }
            return null
        }

        private fun resolveChildClassByConvention(parentClass: Class<*>, name: String): Class<*>? {
            val field = findCollectionFieldByCandidates(parentClass, name) ?: return null
            val pType = field.genericType as? java.lang.reflect.ParameterizedType ?: return null
            return pType.actualTypeArguments.firstOrNull() as? Class<*>
        }
        private suspend fun countChildrenByParentIdsGeneric(
            template: R2dbcEntityTemplate,
            table: String,
            fkField: String,
            idsIn: List<Any?>
        ): Map<Any?, Long> {
            val ids = idsIn.filterNotNull()
            if (ids.isEmpty()) return emptyMap()

            // (?, ?, ?, ...) 플레이스홀더 동적 생성
            val placeholders = List(ids.size) { "?" }.joinToString(",")
            val sql = ("""
                SELECT """ + camelToSnake(fkField) + """ AS pid, COUNT(*) AS cnt
                FROM """ + table + """
                WHERE """ + camelToSnake(fkField) + """ IN (""" + placeholders + """)
                GROUP BY """ + camelToSnake(fkField) + """
            """).trimIndent()

            var spec = template.databaseClient.sql(sql)
            ids.forEachIndexed { i, v -> spec = spec.bind(i, v) }

            return spec.map { row, _ ->
                val pid = row.get("pid")
                val cnt = (row.get("cnt") as Number).toLong()
                pid to cnt
            }.all().collectList().awaitSingle().toMap()
        }

        private fun resolveTableName(clazz: Class<*>): String {
            // @Table("tb_point") 같은 애노테이션이 있으면 그 값 사용
            clazz.getAnnotation(Table::class.java)?.let { ann ->
                val v = ann.value
                if (v.isNotBlank()) return v
            }
            // 없으면 클래스명을 스네이크 케이스로 추정 (PointLog -> point_log)
            return camelToSnake(clazz.simpleName)
        }
        private fun camelToSnake(s: String): String =
            s.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
                .lowercase()
        /** 자동등록: parentClass.name 컬렉션 필드로 관계를 만들고, childClass 반환 */
        @Suppress("UNCHECKED_CAST")
        private fun autoRegisterByConvention(parentClass: Class<*>, name: String): Class<*> {
            val field = getFieldOrNull(parentClass, name)
                ?: error("Relation '$name' not registered and no field/collection '$name' found on ${parentClass.simpleName}.")

            // 1. 어노테이션 먼저 확인
            when {
                field.isAnnotationPresent(HasMany::class.java) -> {
                    val ann = field.getAnnotation(HasMany::class.java)
                    return registerHasManyAnnotation(parentClass, field, ann)
                }
                field.isAnnotationPresent(HasOne::class.java) -> {
                    val ann = field.getAnnotation(HasOne::class.java)
                    return registerHasOneAnnotation(parentClass, field, ann)
                }
                field.isAnnotationPresent(BelongsTo::class.java) -> {
                    val ann = field.getAnnotation(BelongsTo::class.java)
                    return registerBelongsToAnnotation(parentClass, field, ann)
                }
                field.isAnnotationPresent(ManyToMany::class.java) -> {
                    val ann = field.getAnnotation(ManyToMany::class.java)
                    return registerManyToManyAnnotation(parentClass, field, ann)
                }
            }

            // 2. 어노테이션 없으면 기존 네이밍 규칙 기반 처리
            return autoRegisterByConventionFallback(parentClass, name)
        }
        /** 자동등록: parentClass.name 컬렉션 필드로 관계를 만들고, childClass 반환 */
        @Suppress("UNCHECKED_CAST")
        private fun autoRegisterByConventionFallback(parentClass: Class<*>, name: String): Class<*> {
            // 1) 컬렉션 필드 → OneToMany
            findCollectionFieldByCandidates(parentClass, name)?.let { field ->
                val (childClass, fkField, parentIdField, parentIdOf, childParentIdOf) =
                    inferO2M(parentClass, field)
                val fetcher: suspend (List<Any?>) -> List<Any> = { ids ->
                    val q = Query.query(Criteria.where(fkField).`in`(ids))
                    template.select(childClass as Class<Any>).matching(q).flow().toList()
                }
                val counter: suspend (List<Any?>) -> Map<Any?, Long> = { ids ->
                    countChildrenByParentIdsGeneric(template, resolveTableName(childClass), fkField, ids)
                }
                val setter: (Any, List<Any>) -> Unit = { parent, children -> field.set(parent, children) }
                val rel = SimpleOneToMany<Any, Any?, Any>(
                    relationName = name,
                    parentIdOf = parentIdOf,
                    fetchChildrenByParentIds = fetcher,
                    countChildrenByParentIds = counter,
                    childParentIdOf = childParentIdOf,
                    setChildrenOnParent = setter,
                    childClass = childClass
                )
                relations.registerOneToMany(parentClass as Class<Any>, rel)
                return childClass
            }

            // 2) 단일 필드가 있으면 HasOne 또는 BelongsTo
            val single = getFieldOrNull(parentClass, name)
                ?: error("Relation '$name' not registered and no field/collection '$name' found on ${parentClass.simpleName}.")

            // HasOne 판단: single.type(=child)에 parent FK가 존재하면 HasOne
            val childClass = single.type
            val parentSimple = parentClass.simpleName
            val parentFk = lowerCamel(parentSimple) + "Id"
            val hasOne = runCatching { childClass.getDeclaredField(parentFk) }.isSuccess

            if (hasOne) {
                val parentIdField = resolveIdFieldName(parentClass)
                val parentIdOf: (Any) -> Any? = { p -> parentClass.getDeclaredField(parentIdField).apply { isAccessible = true }.get(p) }
                val fetcher: suspend (List<Any?>) -> Map<Any?, Any?> = { ids ->
                    if (ids.isEmpty()) emptyMap()
                    else {
                        val q = Query.query(Criteria.where(parentFk).`in`(ids))
                        val rows = template.select(childClass as Class<Any>).matching(q).flow().toList()
                        rows.groupBy { childClass.getDeclaredField(parentFk).apply { isAccessible = true }.get(it) }
                            .mapValues { it.value.firstOrNull() }
                    }
                }
                val setter: (Any, Any?) -> Unit = { parent, child -> single.set(parent, child) }
                val rel = SimpleHasOne<Any, Any?, Any>(
                    relationName = name,
                    parentIdOf = parentIdOf,
                    fetchChildByParentIds = fetcher,
                    setChildOnParent = setter,
                    childClass = childClass
                )
                relations.registerHasOne(parentClass as Class<Any>, rel)
                return childClass
            } else {
                // BelongsTo 판단: parent FK가 현재(parentClass) 쪽에 없고, 현재 클래스에 'nameId'가 있을 때
                val targetSimple = childClass.simpleName
                val fkOnCurrent = lowerCamel(targetSimple) + "Id"
                val fkField = getFieldOrNull(parentClass, fkOnCurrent)
                    ?: error("Auto register failed for '$name'. Expected FK '$fkOnCurrent' on ${parentClass.simpleName} for BelongsTo.")
                val parentIdField = resolveIdFieldName(childClass)
                val childParentIdOf: (Any) -> Any? = { c -> parentClass.getDeclaredField(fkOnCurrent).apply { isAccessible = true }.get(c) }
                val parentIdOfParent: (Any) -> Any? = { p -> childClass.getDeclaredField(parentIdField).apply { isAccessible = true }.get(p) }
                val fetcher: suspend (List<Any?>) -> List<Any> = { ids ->
                    if (ids.isEmpty()) emptyList()
                    else {
                        val q = Query.query(Criteria.where(parentIdField).`in`(ids))
                        template.select(childClass as Class<Any>).matching(q).flow().toList()
                    }
                }
                val setter: (Any, Any?) -> Unit = { child, parent -> single.set(child, parent) }
                val rel = SimpleBelongsTo<Any, Any?, Any>(
                    relationName = name,
                    childParentIdOf = childParentIdOf,
                    fetchParentsByIds = fetcher,
                    parentIdOfParent = parentIdOfParent,
                    setParentOnChild = setter,
                    parentClass = childClass
                )
                relations.registerBelongsTo(parentClass as Class<Any>, rel)
                return childClass
            }
        }
        private fun registerHasManyAnnotation(
            parentClass: Class<*>,
            field: Field,
            ann: HasMany
        ): Class<*> {
            field.isAccessible = true

            val childClass = ann.target.java
            val foreignKey = if (ann.foreignKey.isBlank())
                "${lowerCamel(parentClass.simpleName)}Id" else ann.foreignKey
            val localKey = ann.localKey

            val parentIdOf: (Any) -> Any? = { p ->
                parentClass.getDeclaredField(localKey).apply { isAccessible = true }.get(p)
            }

            val childParentIdOf: (Any) -> Any? = { c ->
                childClass.getDeclaredField(foreignKey).apply { isAccessible = true }.get(c)
            }

            val fetcher: suspend (List<Any?>) -> List<Any> = { ids ->
                if (ids.isEmpty()) emptyList()
                else {
                    val q = Query.query(Criteria.where(foreignKey).`in`(ids))
                    template.select(childClass as Class<Any>).matching(q).flow().toList()
                }
            }

            val counter: suspend (List<Any?>) -> Map<Any?, Long> = { ids ->
                countChildrenByParentIdsGeneric(template, resolveTableName(childClass), foreignKey, ids)
            }

            val setter: (Any, List<Any>) -> Unit = { parent, children ->
                field.set(parent, children)
            }

            val rel = SimpleOneToMany<Any, Any?, Any>(
                relationName = field.name,
                parentIdOf = parentIdOf,
                fetchChildrenByParentIds = fetcher,
                countChildrenByParentIds = counter,
                childParentIdOf = childParentIdOf,
                setChildrenOnParent = setter,
                childClass = childClass
            )
            relations.registerOneToMany(parentClass as Class<Any>, rel)
            return childClass
        }
        private fun registerHasOneAnnotation(
            parentClass: Class<*>,
            field: Field,
            ann: HasOne
        ): Class<*> {
            field.isAccessible = true

            val childClass = ann.target.java
            val foreignKey = if (ann.foreignKey.isBlank())
                "${lowerCamel(parentClass.simpleName)}Id" else ann.foreignKey
            val localKey = ann.localKey

            val parentIdOf: (Any) -> Any? = { p ->
                parentClass.getDeclaredField(localKey).apply { isAccessible = true }.get(p)
            }

            val fetcher: suspend (List<Any?>) -> Map<Any?, Any?> = { ids ->
                if (ids.isEmpty()) emptyMap()
                else {
                    val q = Query.query(Criteria.where(foreignKey).`in`(ids))
                    val rows = template.select(childClass as Class<Any>).matching(q).flow().toList()
                    rows.groupBy { childClass.getDeclaredField(foreignKey).apply { isAccessible = true }.get(it) }
                        .mapValues { it.value.firstOrNull() }
                }
            }

            val setter: (Any, Any?) -> Unit = { parent, child ->
                field.set(parent, child)
            }

            val rel = SimpleHasOne<Any, Any?, Any>(
                relationName = field.name,
                parentIdOf = parentIdOf,
                fetchChildByParentIds = fetcher,
                setChildOnParent = setter,
                childClass = childClass
            )
            relations.registerHasOne(parentClass as Class<Any>, rel)
            return childClass
        }
        private fun registerBelongsToAnnotation(
            parentClass: Class<*>,
            field: Field,
            ann: BelongsTo
        ): Class<*> {
            field.isAccessible = true

            val targetClass = ann.target.java
            val fkOnCurrent = if (ann.foreignKey.isBlank())
                "${lowerCamel(targetClass.simpleName)}Id" else ann.foreignKey
            val ownerKey = ann.ownerKey

            val childParentIdOf: (Any) -> Any? = { c ->
                parentClass.getDeclaredField(fkOnCurrent).apply { isAccessible = true }.get(c)
            }

            val parentIdOfParent: (Any) -> Any? = { p ->
                targetClass.getDeclaredField(ownerKey).apply { isAccessible = true }.get(p)
            }

            val fetcher: suspend (List<Any?>) -> List<Any> = { ids ->
                if (ids.isEmpty()) emptyList()
                else {
                    val q = Query.query(Criteria.where(ownerKey).`in`(ids))
                    template.select(targetClass as Class<Any>).matching(q).flow().toList()
                }
            }

            val setter: (Any, Any?) -> Unit = { child, parent ->
                field.set(child, parent)
            }

            val rel = SimpleBelongsTo<Any, Any?, Any>(
                relationName = field.name,
                childParentIdOf = childParentIdOf,
                fetchParentsByIds = fetcher,
                parentIdOfParent = parentIdOfParent,
                setParentOnChild = setter,
                parentClass = targetClass
            )
            relations.registerBelongsTo(parentClass as Class<Any>, rel)
            return targetClass
        }
        private fun registerManyToManyAnnotation(
            parentClass: Class<*>,
            field: Field,
            ann: ManyToMany
        ): Class<*> {
            field.isAccessible = true

            val targetClass = ann.target.java
            val joinTable = ann.joinTable
            val localKey = ann.localKey
            val targetKey = ann.targetKey
            val foreignKey = ann.foreignKey
            val relatedKey = ann.relatedKey

            // 현재 모델의 PK 값
            val parentIdOf: (Any) -> Any? = { parent ->
                parentClass.getDeclaredField(localKey).apply { isAccessible = true }.get(parent)
            }

            // ManyToMany 조회
            val fetcher: suspend (List<Any?>) -> Map<Any?, List<Any>> = { parentIds ->
                if (parentIds.isEmpty()) emptyMap()
                else {
                    val placeholders = List(parentIds.size) { "?" }.joinToString(",")
                    val sql = """
                SELECT t.*,
                       jt.$foreignKey AS parent_id
                FROM $joinTable jt
                JOIN ${resolveTableName(targetClass)} t ON t.$targetKey = jt.$relatedKey
                WHERE jt.$foreignKey IN ($placeholders)
            """.trimIndent()

                    var spec = template.databaseClient.sql(sql)
                    parentIds.forEachIndexed { i, v -> spec = spec.bind(i, v) }

                    val result = spec.map { row, _ ->
                        val parentId = row.get("parent_id")
                        val targetObj = targetClass.getDeclaredConstructor().newInstance()

                        // 대상 모델의 필드 채우기
                        targetClass.declaredFields.forEach { f ->
                            f.isAccessible = true
                            row.get(f.name)?.let { f.set(targetObj, it) }
                        }
                        parentId to targetObj
                    }.all().collectList().awaitSingle()

                    result.groupBy({ it.first }, { it.second })
                }
            }

            // setter
            val setter: (Any, List<Any>) -> Unit = { parent, children ->
                field.isAccessible = true
                field.set(parent, children)
            }

            val rel = kr.jiasoft.hiteen.eloquent.relations.SimpleManyToMany<Any, Any?, Any>(
                relationName = field.name,
                parentIdOf = parentIdOf,
                fetchChildrenByParentIds = fetcher,
                setChildrenOnParent = setter,
                childClass = targetClass
            )

            relations.registerManyToMany(parentClass as Class<Any>, rel)
            return targetClass
        }
        private fun inferO2M(parentClass: Class<*>, field: java.lang.reflect.Field): O2MInfo {
            val pType = field.genericType as? java.lang.reflect.ParameterizedType
                ?: error("Field '${field.name}' must be generic Collection like List<Post>.")
            val childClass = (pType.actualTypeArguments.firstOrNull() as? Class<*>)
                ?: error("Cannot resolve generic type for '${field.name}'.")
            val parentSimple = parentClass.simpleName
            val defaultFk = lowerCamel(parentSimple) + "Id"
            val fkField = when {
                runCatching { childClass.getDeclaredField(defaultFk) }.isSuccess -> defaultFk
                else -> {
                    val candidates = childClass.declaredFields.map { it.name }.filter { it.endsWith("Id", true) }
                    candidates.firstOrNull { it.startsWith(lowerCamel(parentSimple)) }
                        ?: error("Auto register failed for '${field.name}'. Expected FK like '$defaultFk' on ${childClass.simpleName}.")
                }
            }
            val parentIdField = resolveIdFieldName(parentClass)
            val parentIdOf: (Any) -> Any? = { p -> parentClass.getDeclaredField(parentIdField).apply { isAccessible = true }.get(p) }
            val childParentIdOf: (Any) -> Any? = { c -> childClass.getDeclaredField(fkField).apply { isAccessible = true }.get(c) }
            return O2MInfo(childClass, fkField, parentIdField, parentIdOf, childParentIdOf)
        }
    }

}
