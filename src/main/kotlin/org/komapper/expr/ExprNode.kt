package org.komapper.expr

import kotlin.reflect.KClass
import org.komapper.expr.ExprLocation as Loc

sealed class ExprNode {
    abstract val location: Loc

    data class Not(override val location: Loc, val operand: ExprNode) : ExprNode()
    data class Or(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class And(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Eq(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Ne(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Ge(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Le(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Gt(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Lt(override val location: Loc, val left: ExprNode, val right: ExprNode) : ExprNode()
    data class Property(override val location: Loc, val name: String, val receiver: ExprNode) : ExprNode()
    data class Function(override val location: Loc, val name: String, val receiver: ExprNode, val args: ExprNode) :
        ExprNode()

    data class Comma(override val location: Loc, val nodeList: List<ExprNode>) : ExprNode()
    data class Value(override val location: Loc, val name: String) : ExprNode()
    data class Literal(override val location: Loc, val value: Any?, val kClass: KClass<out Any>) : ExprNode()
    data class Empty(override val location: Loc) : ExprNode()
}

