package koma.sql

import koma.sql.SqlTokenType.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class SqlParser constructor(
    val sql: String,
    private val tokenizer: SqlTokenizer = SqlTokenizer(sql)
) {

    private val reducers: LinkedList<SqlReducer> = LinkedList()
    private var tokenType: SqlTokenType = EOF
    private var token: String = ""
    private val location: SqlLocation
        get() = tokenizer.location

    init {
        reducers.push(StatementReducer())
    }

    fun parse(): SqlNode {
        outer@ while (true) {
            tokenType = tokenizer.next()
            token = tokenizer.token
            when (tokenType) {
                DELIMITER, EOF -> break@outer
                OPEN_BRACKET -> {
                    val parser = SqlParser(sql, tokenizer)
                    val node = parser.parse()
                    if (parser.tokenType != CLOSE_BRACKET) {
                        throw SqlException("The close bracket is not found at $location")
                    }
                    pushNode(BracketsNode(node))
                }
                CLOSE_BRACKET -> break@outer
                WORD, QUOTE -> pushNode(WordNode(token))
                WHITESPACE -> pushNode(WhitespacesNode.of(token))
                OTHER, EOL -> pushNode(OtherNode.of(token))
                MULTI_LINE_COMMENT, SINGLE_LINE_COMMENT -> pushNode(CommentNode(token))
                SELECT -> reducers.push(SelectReducer(location, token))
                FROM -> reducers.push(FromReducer(location, token))
                WHERE -> reducers.push(WhereReducer(location, token))
                GROUP_BY -> reducers.push(GroupByReducer(location, token))
                HAVING -> reducers.push(HavingReducer(location, token))
                ORDER_BY -> reducers.push(OrderByReducer(location, token))
                FOR_UPDATE -> reducers.push(ForUpdateReducer(location, token))
                OPTION -> reducers.push(OptionReducer(location, token))
                AND -> reducers.push(AndReducer(location, token))
                OR -> reducers.push(OrReducer(location, token))
                UNION, EXCEPT, MINUS, INTERSECT -> {
                    val node = reduceAll()
                    reducers.push(SetReducer(location, token, node))
                    reducers.push(StatementReducer())
                }
                BIND_VALUE_DIRECTIVE -> parseBindValueDirective()
                LITERAL_VALUE_DIRECTIVE -> parseLiteralValueDirective()
                EMBEDDED_VALUE_DIRECTIVE -> parseEmbeddedValueDirective()
                IF_DIRECTIVE -> parseIfDirective()
                ELSEIF_DIRECTIVE -> parseElseifDirective()
                ELSE_DIRECTIVE -> parseElseDirective()
                END_DIRECTIVE -> parseEndDirective()
                FOR_DIRECTIVE -> parseForDirective()
                EXPAND_DIRECTIVE -> parseExpandDirective()
            }
        }
        return reduceAll()
    }

    private fun parseBindValueDirective() {
        val expression = token.substring(2, token.length - 2).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the bind value directive at $location")
        }
        reducers.push(BindValueDirectiveReducer(location, token, expression))
    }

    private fun parseLiteralValueDirective() {
        val expression = token.substring(3, token.length - 2).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the literal value directive at $location")
        }
        reducers.push(LiteralValueDirectiveReducer(location, token, expression))
    }

    private fun parseEmbeddedValueDirective() {
        val expression = token.substring(3, token.length - 2).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the embedded value directive at $location")
        }
        pushNode(EmbeddedValueDirectiveNode(location, token, expression))
    }

    private fun parseIfDirective() {
        val expression = token.substring(5, token.length - 2).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the if directive at $location")
        }
        reducers.push(IfBlockReducer(location))
        reducers.push(IfDirectiveReducer(location, token, expression))
    }

    private fun parseElseifDirective() {
        val expression = token.substring(9, token.length - 2).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the elseif directive at $location")
        }
        reduceUntil(IfBlockReducer::class)
        if (reducers.isEmpty()) {
            throw SqlException("The corresponding if directive is not found at $location")
        }
        reducers.push(ElseifDirectiveReducer(location, token, expression))
    }

    private fun parseElseDirective() {
        reduceUntil(IfBlockReducer::class)
        if (reducers.isEmpty()) {
            throw SqlException("The corresponding if directive is not found at $location")
        }
        reducers.push(ElseDirectiveReducer(location, token))
    }

    private fun parseEndDirective() {
        reduceUntil(BlockReducer::class)
        if (reducers.isEmpty()) {
            throw SqlException("The corresponding if or for directive is not found at $location")
        }
        pushNode(EndDirectiveNode(location, token))
        val block = reducers.pop()
        pushNode(block.reduce())
    }

    private fun parseForDirective() {
        val statement = token.substring(6, token.length - 2).trim()
        if (statement.isEmpty()) {
            throw SqlException("The statement is not found in the for directive at $location")
        }
        val pos = statement.indexOf("in")
        if (pos == -1) {
            throw SqlException("The keyword \"in\" is not found in the statement in the for directive at $location")
        }
        val identifier = statement.substring(0, pos).trim()
        if (identifier.isEmpty()) {
            throw SqlException("The identifier is not found in the statement in the for directive at $location")
        }
        val expression = statement.substring(pos + 1).trim()
        if (expression.isEmpty()) {
            throw SqlException("The expression is not found in the statement in the for directive at $location")
        }
        reducers.push(ForBlockReducer(location))
        reducers.push(ForDirectiveReducer(location, token, identifier, expression))
    }

    private fun parseExpandDirective() {
        val expression = token.substring(9, token.length - 2).trim()
        reducers.push(ExpandDirectiveReducer(location, token, expression))

        // TODO
//        var alias = tokenType.extract(token)
//        if (alias.isEmpty() || StringUtil.isWhitespace(alias)) {
//            alias = "\"\""
//        }
//        val node = ExpandNode(location, alias, token)
    }


    private fun reduceUntil(kClass: KClass<out SqlReducer>) {
        val it = reducers.iterator()
        while (it.hasNext()) {
            val reducer = it.next()
            if (reducer::class.isSubclassOf(kClass)) {
                break
            }
            it.remove()
            val node = reducer.reduce()
            pushNode(node)
        }
    }

    private fun reduceAll(): SqlNode {
        val it = reducers.iterator()
        var node: SqlNode? = null
        while (it.hasNext()) {
            val reducer = it.next()
            it.remove()
            node = reducer.reduce()
            pushNode(node)
        }
        return node ?: throw AssertionError("no reducers")
    }

    private fun pushNode(node: SqlNode) {
        reducers.peek()?.addNode(node)
    }

}
