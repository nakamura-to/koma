package org.komapper.expr

import org.komapper.expr.ExprTokenType.*
import java.math.BigDecimal
import java.util.*

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
                OPEN_BRACKET -> parseBrackets()
                CLOSE_BRACKET -> break@outer
                WHITESPACE -> {
                }
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
            null -> EmptyNode(location)
            else -> node
        }
    }

    private fun parseValue() {
        val node = ValueNode(location, token)
        nodes.push(node)
    }

    private fun parseBrackets() {
        val parser = ExprParser(expression, tokenizer)
        val node = parser.parse()
        if (parser.tokenType != CLOSE_BRACKET) {
            throw ExprException("The close bracket is not found at $location")
        }
        nodes.push(node)
    }

    private fun parseStringLiteral() {
        val value = token.substring(1, token.length - 1)
        val node = LiteralNode(location, value, String::class)
        nodes.push(node)
    }

    private fun parseCharLiteral() {
        val value = token[1]
        val node = LiteralNode(location, value, Char::class)
        nodes.push(node)
    }

    private fun parseIntLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length
        val value = Integer.valueOf(token.substring(start, end))
        val node = LiteralNode(location, value, Int::class)
        nodes.push(node)
    }

    private fun parseLongLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Long.valueOf(token.substring(start, end))
        val node = LiteralNode(location, value, Long::class)
        nodes.push(node)
    }

    private fun parseFloatLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Float.valueOf(token.substring(start, end))
        val node = LiteralNode(location, value, Float::class)
        nodes.push(node)
    }

    private fun parseDoubleLiteral() {
        val start = if (token[0] == '+') 1 else 0
        val end = token.length - 1
        val value = java.lang.Double.valueOf(token.substring(start, end))
        val node = LiteralNode(location, value, Double::class)
        nodes.push(node)
    }

    private fun parseBigDecimalLiteral() {
        val start = 0
        val end = token.length - 1
        val value = BigDecimal(token.substring(start, end))
        val node = LiteralNode(location, value, BigDecimal::class)
        nodes.push(node)
    }

    private fun parseTrueLiteral() {
        val node = LiteralNode(location, true, Boolean::class)
        nodes.push(node)
    }

    private fun parseFalseLiteral() {
        val node = LiteralNode(location, false, Boolean::class)
        nodes.push(node)
    }

    private fun parseNullLiteral() {
        val node = LiteralNode(location, null, Any::class)
        nodes.push(node)
    }

    private fun parseFunction() {
        val name = token.substring(1)
        val reducer = FunctionReducer(location, name)
        tokenType = tokenizer.next()
        parseBrackets()
        pushReducer(reducer)
    }

    private fun parseProperty() {
        val name = token.substring(1)
        val reducer = PropertyReducer(location, name)
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