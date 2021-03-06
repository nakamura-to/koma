package org.komapper.core.sql

import kotlin.reflect.KClass
import org.komapper.core.expr.ExprEvaluator
import org.komapper.core.expr.ExprException
import org.komapper.core.value.Value

interface StmtBuilder {
    fun build(
        template: Template<*>,
        expander: (String) -> List<String> = { emptyList() }
    ): Stmt
}

open class DefaultStmtBuilder(
    private val formatter: (Any?, KClass<*>) -> String,
    private val anyDescFactory: AnyDescFactory,
    private val sqlNodeFactory: SqlNodeFactory,
    private val exprEvaluator: ExprEvaluator
) : StmtBuilder {

    private val clauseRegex = Regex(
        """^(select|from|where|group by|having|order by|for update|option)\s""", RegexOption.IGNORE_CASE
    )

    override fun build(template: Template<*>, expander: (String) -> List<String>): Stmt {
        val (sql, args) = template
        val ctx = anyDescFactory.toMap(args)
        return build(sql, ctx, expander)
    }

    fun build(
        sql: CharSequence,
        ctx: Map<String, Value> = emptyMap(),
        expander: (String) -> List<String> = { emptyList() }
    ): Stmt {
        val node = sqlNodeFactory.get(sql)
        val state = visit(State(ctx, expander), node)
        return state.toStmt()
    }

    private fun visit(state: State, node: SqlNode): State = when (node) {
        is SqlNode.Statement -> node.nodeList.fold(state, ::visit)
        is SqlNode.Set -> {
            val left = visit(State(state), node.left)
            if (left.available) {
                state.append(left)
            }
            val right = visit(State(state), node.right)
            if (right.available) {
                if (left.available) {
                    state.append(node.keyword)
                }
                state.append(right)
            }
            state
        }
        is SqlNode.Clause.Select -> {
            state.append(node.keyword)
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.Clause.From -> {
            state.append(node.keyword)
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.Clause.ForUpdate -> {
            state.append(node.keyword)
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.Clause -> {
            val childState = node.nodeList.fold(State(state), ::visit)
            if (childState.available) {
                state.append(node.keyword).append(childState)
            } else if (childState.startsWithClause()) {
                state.available = true
                state.append(childState)
            }
            state
        }
        is SqlNode.BiLogicalOp -> {
            if (state.available) {
                state.append(node.keyword)
            }
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.Token -> {
            if (node is SqlNode.Token.Word || node is SqlNode.Token.Other) {
                state.available = true
            }
            state.append(node.token)
        }
        is SqlNode.Paren -> {
            state.available = true
            state.append("(")
            visit(state, node.node).append(")")
        }
        is SqlNode.BindValueDirective -> {
            val result = eval(node.location, node.expression, state.ctx)
            when (val obj = result.obj) {
                is Iterable<*> -> {
                    var counter = 0
                    state.append("(")
                    for (o in obj) {
                        if (++counter > 1) state.append(", ")
                        when (o) {
                            is Pair<*, *> -> {
                                val (f, s) = o
                                state.append("(")
                                    .bind(newValue(f)).append(", ")
                                    .bind(newValue(s)).append(")")
                            }
                            is Triple<*, *, *> -> {
                                val (f, s, t) = o
                                state.append("(")
                                    .bind(newValue(f)).append(", ")
                                    .bind(newValue(s)).append(", ")
                                    .bind(newValue(t)).append(")")
                            }
                            else -> state.bind(newValue(o))
                        }
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
        is SqlNode.EmbeddedValueDirective -> {
            val (obj) = eval(node.location, node.expression, state.ctx)
            val s = obj?.toString()
            if (!s.isNullOrEmpty()) {
                state.available = true
                state.append(s)
            }
            state
        }
        is SqlNode.LiteralValueDirective -> {
            val (obj, type) = eval(node.location, node.expression, state.ctx)
            val literal = formatter(obj, type)
            state.append(literal)
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.ExpandDirective -> {
            state.available = true
            val (obj) = eval(node.location, node.expression, state.ctx)
            if (obj == null) {
                throw SqlException("The alias expression \"${node.expression}\" cannot be resolved at ${node.location}.")
            }
            val alias = obj.toString()
            val prefix = if (alias.isEmpty()) "" else "$alias."
            val columns = state.expander(prefix).joinToString()
            state.append(columns)
            node.nodeList.fold(state, ::visit)
        }
        is SqlNode.IfBlock -> {
            fun chooseNodeList(): List<SqlNode> {
                val (result) = eval(node.ifDirective.location, node.ifDirective.expression, state.ctx)
                if (result == true) {
                    return node.ifDirective.nodeList
                } else {
                    val elseIfDirective = node.elseifDirectives.find {
                        val (r) = eval(it.location, it.expression, state.ctx)
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
        is SqlNode.ForBlock -> {
            val forDirective = node.forDirective
            val id = forDirective.identifier
            val (expression) = eval(node.forDirective.location, node.forDirective.expression, state.ctx)
            expression as? Iterable<*>
                ?: throw SqlException("The expression ${forDirective.expression} is not Iterable at ${forDirective.location}")
            val it = expression.iterator()
            var s = state
            val preserved = s.ctx[id]
            var index = 0
            val idIndex = id + "_index"
            val idHasNext = id + "_has_next"
            while (it.hasNext()) {
                val each = it.next()
                s.ctx[id] = newValue(each)
                s.ctx[idIndex] = Value(index++, Int::class)
                s.ctx[idHasNext] = Value(it.hasNext(), Boolean::class)
                s = node.forDirective.nodeList.fold(s, ::visit)
            }
            if (preserved != null) {
                s.ctx[id] = preserved
            }
            s.ctx.remove(idIndex)
            s.ctx.remove(idHasNext)
            s
        }
        is SqlNode.IfDirective,
        is SqlNode.ElseifDirective,
        is SqlNode.ElseDirective,
        is SqlNode.EndDirective,
        is SqlNode.ForDirective -> error("unreachable")
    }

    private fun newValue(o: Any?) = Value(o, o?.let { it::class } ?: Any::class)

    private fun eval(location: SqlLocation, expression: String, ctx: Map<String, Value>): Value = try {
        exprEvaluator.eval(expression, ctx)
    } catch (e: ExprException) {
        throw SqlException("The expression evaluation was failed at $location.", e)
    }

    inner class State(ctx: Map<String, Value>, val expander: (String) -> List<String>) {
        constructor(state: State) : this(state.ctx, state.expander)

        private val buf = StmtBuffer(formatter)
        val ctx: MutableMap<String, Value> = HashMap(ctx)
        var available: Boolean = false

        fun append(state: State): State {
            buf.sql.append(state.buf.sql)
            buf.log.append(state.buf.log)
            buf.values.addAll(state.buf.values)
            return this
        }

        fun append(s: CharSequence): State {
            buf.append(s)
            return this
        }

        fun bind(value: Value): State {
            buf.bind(value)
            return this
        }

        fun startsWithClause(): Boolean {
            val s = buf.toString().trim()
            return clauseRegex.containsMatchIn(s)
        }

        fun toStmt() = buf.toStmt()

        override fun toString() = buf.toString()
    }
}
