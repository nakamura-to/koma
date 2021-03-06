package org.komapper.core.entity

import kotlin.reflect.KFunction

data class DataDesc<T : Any>(
    val entityMeta: EntityMeta<T>,
    private val constructor: KFunction<T>,
    private val copy: KFunction<*>,
    private val propDescList: List<PropDesc>,
    private val isMarkedNullable: Boolean
) {
    fun new(leaves: Map<PropDesc, Any?>): T {
        val args = propDescList.map { it.constructorParam to it.new(leaves) }.toMap()
        return constructor.callBy(args)
    }

    fun copy(
        receiver: Any,
        predicate: (PropDesc) -> Boolean,
        block: (PropDesc, () -> Any?) -> Any?
    ): Any? {
        val valueArgs = propDescList.mapNotNull { it.copy(receiver, predicate, block) }.toMap()
        return if (valueArgs.isEmpty() && isMarkedNullable) {
            null
        } else {
            val receiverArg = copy.parameters[0] to receiver
            copy.callBy(mapOf(receiverArg) + valueArgs)
        }
    }

    fun getLeafPropDescList(): List<PropDesc> =
        propDescList.flatMap { it.getLeafPropDescList() }
}
