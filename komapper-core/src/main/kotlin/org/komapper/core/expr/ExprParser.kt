package org.komapper.core.expr

import java.math.BigDecimal
import java.util.Deque
import java.util.LinkedList
import org.komapper.core.expr.ExprTokenType.AND
import org.komapper.core.expr.ExprTokenType.BIG_DECIMAL
import org.komapper.core.expr.ExprTokenType.CHAR
import org.komapper.core.expr.ExprTokenType.CLASS_REF
import org.komapper.core.expr.ExprTokenType.CLOSE_PAREN
import org.komapper.core.expr.ExprTokenType.COMMA
import org.komapper.core.expr.ExprTokenType.DOUBLE
import org.komapper.core.expr.ExprTokenType.EOE
import org.komapper.core.expr.ExprTokenType.EQ
import org.komapper.core.expr.ExprTokenType.FALSE
import org.komapper.core.expr.ExprTokenType.FLOAT
import org.komapper.core.expr.ExprTokenType.FUNCTION
import org.komapper.core.expr.ExprTokenType.GE
import org.komapper.core.expr.ExprTokenType.GT
import org.komapper.core.expr.ExprTokenType.ILLEGAL_NUMBER
import org.komapper.core.expr.ExprTokenType.INT
import org.komapper.core.expr.ExprTokenType.LE
import org.komapper.core.expr.ExprTokenType.LONG
import org.komapper.core.expr.ExprTokenType.LT
import org.komapper.core.expr.ExprTokenType.NE
import org.komapper.core.expr.ExprTokenType.NOT
import org.komapper.core.expr.ExprTokenType.NULL
import org.komapper.core.expr.ExprTokenType.OPEN_PAREN
import org.komapper.core.expr.ExprTokenType.OR
import org.komapper.core.expr.ExprTokenType.OTHER
import org.komapper.core.expr.ExprTokenType.PROPERTY
import org.komapper.core.expr.ExprTokenType.SAFE_CALL_FUNCTION
import org.komapper.core.expr.ExprTokenType.SAFE_CALL_PROPERTY
import org.komapper.core.expr.ExprTokenType.STRING
import org.komapper.core.expr.ExprTokenType.TRUE
import org.komapper.core.expr.ExprTokenType.VALUE
import org.komapper.core.expr.ExprTokenType.WHITESPACE

class ExprParser(
    private val expression: String,
    private val tokenizer: ExprTokenizer = ExprTokenizer(expression)
) {

    private val nodes: Deque<ExprNode> = LinkedList()
    private val reducers: Deque<ExprReducer> = LinkedList()
    private var tokenType = EOE
    private var token = ""
    private val location
        get() = tokenizer.location

    fun parse(): ExprNode {
        outer@ while (true) {
            tokenType = tokenizer.next()
            token = tokenizer.token
            when (tokenType) {
                EOE -> break@outer
                OPEN_PAREN -> parseParen()
                CLOSE_PAREN -> break@outer
                WHITESPACE -> {
                }
                CLASS_REF -> parseClassRef()
                VALUE -> parseValue()
                CHAR -> parseCharLiteral()
                STRING -> parseStringLiteral()
                INT -> parseIntLiteral()
                LONG -> parseLongLiteral()
                FLOAT -> parseFloatLiteral()
                DOUBLE -> parseDoubleLiteral()
                BIG_DECIMAL -> parseBigDecimalLiteral()
                TRUE -> parseTrueLiteral()
                FALSE -> parseFalseLiteral()
                NULL -> parseNullLiteral()
                NOT -> pushReducer(NotReducer(location))
                AND -> pushReducer(AndReducer(location))
                OR -> pushReducer(OrReducer(location))
                COMMA -> pushReducer(CommaReducer(location))
                EQ -> pushReducer(EqReducer(location))
                NE -> pushReducer(NeReducer(location))
                GE -> pushReducer(GeReducer(location))
                LE -> pushReducer(LeReducer(location))
                GT -> pushReducer(GtReducer(location))
                LT -> pushReducer(LtReducer(location))
                SAFE_CALL_FUNCTION -> parseFunction(true)
                SAFE_CALL_PROPERTY -> parseProperty(true)
                FUNCTION -> parseFunction()
                PROPERTY -> parseProperty()
                ILLEGAL_NUMBER ->
                    throw ExprException("The illegal number literal \"$token\" is found at $location")
                OTHER ->
                    throw ExprException("The token \"$token\" is not supported at $location")
            }
        }
        reduceAll()
        return when (val node = nodes.poll()) {
            null -> ExprNode.Empty(location)
            else -> node
        }
    }

    private fun parseClassRef() {
        val name = token.substring(1, token.length - 1)
        val node = ExprNode.ClassRef(location, name)
        nodes.push(node)
    }

    private fun parseValue() {
        val node = ExprNode.Value(location, token)
        nodes.push(node)
    }

    private fun parseParen() {
        val parser = ExprParser(expression, tokenizer)
        val node = parser.parse()
        if (parser.tokenType != CLOSE_PAREN) {
            throw ExprException("The close paren is not found at $location")
        }
        nodes.push(node)
    }

    private fun parseStringLiteral() {
        val value = token.substring(1, token.length - 1)
        val node = ExprNode.Literal(location, value, String::class)
        nodes.push(node)
    }

    private fun parseCharLiteral() {
        val value = token[1]
        val node = ExprNode.Literal(location, value, Char::class)
        nodes.push(node)
    }

    private fun parseIntLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length
        val value = Integer.valueOf(token.substring(start, end))
        val node = ExprNode.Literal(location, value, Int::class)
        nodes.push(node)
    }

    private fun parseLongLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Long.valueOf(token.substring(start, end))
        val node = ExprNode.Literal(location, value, Long::class)
        nodes.push(node)
    }

    private fun parseFloatLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Float.valueOf(token.substring(start, end))
        val node = ExprNode.Literal(location, value, Float::class)
        nodes.push(node)
    }

    private fun parseDoubleLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Double.valueOf(token.substring(start, end))
        val node = ExprNode.Literal(location, value, Double::class)
        nodes.push(node)
    }

    private fun parseBigDecimalLiteral() {
        val start = 0
        val end = token.length - 1
        val value = BigDecimal(token.substring(start, end))
        val node = ExprNode.Literal(location, value, BigDecimal::class)
        nodes.push(node)
    }

    private fun parseTrueLiteral() {
        val node = ExprNode.Literal(location, true, Boolean::class)
        nodes.push(node)
    }

    private fun parseFalseLiteral() {
        val node = ExprNode.Literal(location, false, Boolean::class)
        nodes.push(node)
    }

    private fun parseNullLiteral() {
        val node = ExprNode.Literal(location, null, Any::class)
        nodes.push(node)
    }

    private fun parseFunction(safeCall: Boolean = false) {
        val name = token.substring(if (safeCall) 2 else 1)
        val reducer = FunctionReducer(location, name, safeCall)
        tokenType = tokenizer.next()
        parseParen()
        pushReducer(reducer)
    }

    private fun parseProperty(safeCall: Boolean = false) {
        val name = token.substring(if (safeCall) 2 else 1)
        val reducer = PropertyReducer(location, name, safeCall)
        pushReducer(reducer)
    }

    private fun pushReducer(reducer: ExprReducer) {
        if (reducers.isNotEmpty()) {
            val first = reducers.peek()
            if (first.priority > reducer.priority) {
                val it = reducers.iterator()
                while (it.hasNext()) {
                    val r = it.next()
                    if (r.priority > reducer.priority) {
                        it.remove()
                        reduce(r)
                    }
                }
            }
        }
        reducers.push(reducer)
    }

    private fun reduceAll() {
        val it = reducers.iterator()
        while (it.hasNext()) {
            val reducer = it.next()
            it.remove()
            reduce(reducer)
        }
    }

    private fun reduce(reducer: ExprReducer) {
        val node = reducer.reduce(nodes)
        nodes.push(node)
    }
}
