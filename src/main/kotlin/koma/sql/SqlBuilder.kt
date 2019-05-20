package koma.sql

import koma.expr.ExprEvaluator
import koma.meta.EntityMeta
import koma.meta.PropKind
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

class SqlBuilder(private val evaluator: ExprEvaluator = ExprEvaluator()) {

    fun build(template: String, ctx: Map<String, Pair<*, KClass<*>>> = emptyMap()): Sql {
        val parser = SqlParser(template)
        val node = parser.parse()
        val state = visit(State(ctx), node)
        val buffer = state.getBuffer()
        return Sql(buffer.sql.toString(), buffer.values, buffer.log.toString())
    }

    private fun visit(state: State, node: SqlNode): State = when (node) {
        is Statement -> node.nodeList.fold(state, ::visit)
        is Set -> {
            val left = visit(state, node.left)
            state.append(node.keyword)
            visit(left, node.right)
        }
        is ForUpdate -> {
            state.append(node.keyword)
            node.nodeList.fold(state, ::visit)
        }
        is Keyword -> {
            val childState = node.nodeList.fold(State(state), ::visit)
            if (childState.available) {
                state.append(node.keyword)
                state.append(childState)
            }
            state
        }
        is Token -> {
            if (node is Word || node is Other) {
                state.available = true
            }
            state.append(node.token)
            state
        }
        is Brackets -> {
            state.available = true
            state.append("(")
            visit(state, node.node).also {
                state.append(")")
            }
        }
        is BindValueDirective -> {
            val result = eval(node.expression, state.ctx)
            when (val value = result.first) {
                is Iterable<*> -> {
                    var counter = 0
                    state.append("(")
                    for (v in value) {
                        if (++counter > 1) state.append(", ")
                        state.bind(v to (if (v == null) Any::class else v::class))
                    }
                    if (counter == 0) {
                        state.append("null")
                    }
                    state.append(")")
                }
                else -> state.bind(result)
            }
            node.nodeList.fold(state, ::visit)
        }
        is EmbeddedValueDirective -> {
            val (value) = eval(node.expression, state.ctx)
            val s = value?.toString()
            if (!s.isNullOrEmpty()) {
                state.available = true
                state.append(s)
            }
            state
        }
        is LiteralValueDirective -> {
            val (value) = eval(node.expression, state.ctx)
            val literal = toText(value)
            state.append(literal)
            node.nodeList.fold(state, ::visit)
        }
        is ExpandDirective -> {
            // TODO
            throw NotImplementedError()
        }
        is IfBlock -> {
            fun chooseNodeList(): List<SqlNode> {
                val (result) = eval(node.ifDirective.expression, state.ctx)
                if (result == true) {
                    return node.ifDirective.nodeList
                } else {
                    val elseIfDirective = node.elseifDirectives.find {
                        val (r) = eval(it.expression, state.ctx)
                        r == true
                    }
                    if (elseIfDirective != null) {
                        return elseIfDirective.nodeList
                    } else {
                        if (node.elseDirective != null) {
                            return node.elseDirective.nodeList
                        } else {
                            return emptyList()
                        }
                    }
                }
            }

            val nodeList = chooseNodeList()
            nodeList.fold(state, ::visit)
        }
        is ForBlock -> {
            val forDirective = node.forDirective
            val id = forDirective.identifier
            val (expression) = eval(node.forDirective.expression, state.ctx)
            expression as? Iterable<*>
                ?: throw SqlException("The expression ${forDirective.expression} is not Iterable at ${forDirective.location}")
            val it = expression.iterator()
            var s = state
            var preserved = s.ctx[id]
            var index = 0
            val idIndex = id + "_index"
            val idHasNext = id + "_has_next"
            while (it.hasNext()) {
                val each = it.next()
                s.ctx[id] = if (each == null) null to Any::class else each to each::class
                s.ctx[idIndex] = index++ to Int::class
                s.ctx[idHasNext] = it.hasNext() to Boolean::class
                s = node.forDirective.nodeList.fold(s, ::visit)
            }
            if (preserved != null) {
                s.ctx[id] = preserved
            }
            s.ctx.remove(idIndex)
            s.ctx.remove(idHasNext)
            s
        }
        is IfDirective,
        is ElseifDirective,
        is ElseDirective,
        is EndDirective,
        is ForDirective -> {
            throw AssertionError("unreachable")
        }
    }

    private fun eval(expression: String, ctx: Map<String, Pair<*, KClass<*>>>): Pair<*, KClass<*>> {
        return evaluator.eval(expression, ctx)
    }

    private fun toText(value: Any?): String {
        return if (value is CharSequence) "'$value'" else value.toString()
    }

}

class Buffer(capacity: Int = 200) {

    val sql = StringBuilder(capacity)
    val log = StringBuilder(capacity)
    val values = ArrayList<Pair<*, KClass<*>>>()

    fun append(s: CharSequence) {
        sql.append(s)
        log.append(s)
    }

    fun bind(value: Pair<*, KClass<*>>) {
        sql.append("?")
        log.append(toText(value))
        values.add(value)
    }

    fun cutBack(length: Int) {
        sql.setLength(sql.length - length)
        log.setLength(sql.length - length)
    }

}

class State(ctx: Map<String, Pair<*, KClass<*>>>) {
    var available: Boolean = false
    val ctx: MutableMap<String, Pair<*, KClass<*>>> = HashMap(ctx)
    val buf = Buffer()

    constructor(state: State) : this(state.ctx)

    fun append(state: State) {
        buf.sql.append(state.buf.sql)
        buf.log.append(state.buf.log)
        buf.values.addAll(state.buf.values)
    }

    fun append(s: CharSequence) {
        buf.append(s)
    }

    fun bind(value: Pair<*, KClass<*>>) {
        buf.bind(value)
    }

    fun getBuffer(): Buffer {
        return buf
    }

}

private fun toText(value: Any?): String {
    return if (value is CharSequence) "'$value'" else value.toString()
}

data class Sql(val text: String, val values: List<Pair<*, KClass<*>>>, val log: String)

fun createDeleteSql(entity: Any, entityMeta: EntityMeta): Sql {
    val buf = Buffer()
    buf.append("delete from ${entityMeta.tableName}")
    val idPropList = entityMeta.idPropMetaList
    if (idPropList.isNotEmpty()) {
        buf.append(" where ")
        idPropList.forEach {
            buf.append("${it.columnName} = ")
            buf.bind(it.getValue(entity))
            buf.append(" and ")
        }
        buf.cutBack(5)
    }
    val versionProp = entityMeta.versionPropMeta
    if (versionProp != null) {
        if (idPropList.isEmpty()) {
            buf.append(" where ")
        } else {
            buf.append(" and ")
        }
        buf.append("${versionProp.columnName} = ")
        buf.bind(versionProp.getValue(entity))
    }
    return Sql(buf.sql.toString(), buf.values, buf.log.toString())
}

fun createInsertSql(entity: Any, entityMeta: EntityMeta): Sql {
    var version: Any? = null
    val buf = Buffer()
    buf.append("insert into ${entityMeta.tableName}")
    buf.append(" (")
    val propList = entityMeta.propMetaList
    propList.forEach { prop ->
        buf.append("${prop.columnName}, ")
    }
    buf.cutBack(2)
    buf.append(") values(")
    propList.forEach { prop ->
        buf.bind(prop.getValue(entity))
        buf.append(", ")
    }
    buf.cutBack(2)
    buf.append(")")
    return Sql(buf.sql.toString(), buf.values, buf.log.toString())
}

fun createUpdateSql(entity: Any, entityMeta: EntityMeta): Pair<Sql, Any?> {
    var version: Any? = null
    val buf = Buffer()
    buf.append("update ${entityMeta.tableName}")
    buf.append(" set ")
    val propList = entityMeta.propMetaList
    propList.filter { it.kind != PropKind.Id }.forEach { prop ->
        buf.append("${prop.columnName} = ")
        val value = prop.getValue(entity)
        if (prop.kind == PropKind.Version) {
            val versionValue = increment(value).also { (v) ->
                version = v
            }
            buf.bind(versionValue)
        } else {
            buf.bind(value)
        }
        buf.append(", ")
    }
    buf.cutBack(2)
    val idPropList = entityMeta.idPropMetaList
    if (idPropList.isNotEmpty()) {
        buf.append(" where ")
        idPropList.forEach {
            buf.append("${it.columnName} = ")
            buf.bind(it.getValue(entity))
            buf.append(" and ")
        }
        buf.cutBack(5)
    }
    val versionProp = entityMeta.versionPropMeta
    if (versionProp != null) {
        if (idPropList.isEmpty()) {
            buf.append(" where ")
        } else {
            buf.append(" and ")
        }
        buf.append("${versionProp.columnName} = ")
        buf.bind(versionProp.getValue(entity))
    }
    return Sql(buf.sql.toString(), buf.values, buf.log.toString()) to version
}

@Suppress("IMPLICIT_CAST_TO_ANY")
fun increment(value: Pair<Any?, KClass<*>>): Pair<Any?, KClass<*>> {
    val (first) = value
    val v = when (first) {
        is Byte -> first.inc()
        is Short -> first.inc()
        is Int -> first.inc()
        is Long -> first.inc()
        is BigDecimal -> first.inc()
        is BigInteger -> first.inc()
        else -> TODO()
    }
    return value.copy(v)
}