package org.komapper.core.metadata

import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.komapper.core.desc.EntityListener
import org.komapper.core.dsl.Scope

data class Metadata<T : Any>(
    val kClass: KClass<T>,
    val table: TableMeta = TableMeta(null, false),
    val columnList: List<ColumnMeta> = emptyList(),
    val idList: List<IdMeta> = emptyList(),
    val version: VersionMeta? = null,
    val createdAt: CreatedAtMeta? = null,
    val updatedAt: UpdatedAtMeta? = null,
    val embeddedList: List<EmbeddedMeta> = emptyList(),
    val listener: ListenerMeta<T>? = null
) {
    init {
        require(kClass.isData) { "The kClass \"${kClass.qualifiedName}\" must be a data class." }
    }
}

data class MutableMetadata<T : Any>(
    val kClass: KClass<T>,
    var table: TableMeta = TableMeta(null, false),
    val columnList: MutableList<ColumnMeta> = mutableListOf(),
    val idList: MutableList<IdMeta> = mutableListOf(),
    var version: VersionMeta? = null,
    var createdAt: CreatedAtMeta? = null,
    var updatedAt: UpdatedAtMeta? = null,
    val embeddedList: MutableList<EmbeddedMeta> = mutableListOf(),
    var listener: ListenerMeta<T>? = null
) {
    fun asImmutable(): Metadata<T> = Metadata(
        kClass,
        table,
        columnList,
        idList,
        version,
        createdAt,
        updatedAt,
        embeddedList,
        listener
    )
}

data class TableMeta(val name: String?, val quote: Boolean)
data class ColumnMeta(val propName: String, val name: String?, val quote: Boolean)
data class CreatedAtMeta(val propName: String)
data class UpdatedAtMeta(val propName: String)
sealed class IdMeta {
    abstract val propName: String

    data class Assign(override val propName: String) : IdMeta()
    data class Sequence(
        override val propName: String,
        val generator: SequenceGenerator
    ) : IdMeta()
}

data class VersionMeta(val propName: String)
data class EmbeddedMeta(val propName: String)
data class ListenerMeta<T : Any>(val instance: EntityListener<T>)
data class SequenceGenerator(
    val name: String,
    val incrementBy: Int = 1,
    val quote: Boolean = false
)

@Scope
class EntitiesScope(private val metadataMap: MutableMap<KClass<*>, Metadata<*>>) {

    inline fun <reified T : Any> entity(noinline block: EntityScope<T>.() -> Unit) {
        require(T::class.isData) { "The parameter type T must be a data class." }
        entity(T::class, block)
    }

    fun <T : Any> entity(kClass: KClass<T>, block: EntityScope<T>.() -> Unit) {
        require(kClass.isData) { "The kClass \"${kClass.qualifiedName}\" must be a data class." }
        val metadata = MutableMetadata(kClass).also {
            EntityScope(it).block()
        }
        metadataMap[kClass] = metadata.asImmutable()
    }
}

@Scope
class EntityScope<T : Any>(private val metadata: MutableMetadata<T>) {
    private val tableScope = TableScope(metadata)

    fun id(property: KProperty1<T, *>) {
        val idMeta = IdMeta.Assign(property.name)
        metadata.idList.add(idMeta)
    }

    fun id(
        property: KProperty1<T, Int?>,
        generator: SequenceGenerator,
        @Suppress("UNUSED_PARAMETER") `_`: Int? = null
    ) {
        val idMeta = IdMeta.Sequence(property.name, generator)
        metadata.idList.add(idMeta)
    }

    fun id(
        property: KProperty1<T, Long?>,
        generator: SequenceGenerator,
        @Suppress("UNUSED_PARAMETER") `_`: Long? = null
    ) {
        val idMeta = IdMeta.Sequence(property.name, generator)
        metadata.idList.add(idMeta)
    }

    fun version(
        property: KProperty1<T, Int?>,
        @Suppress("UNUSED_PARAMETER") `_`: Int? = null
    ) {
        metadata.version = VersionMeta(property.name)
    }

    fun version(
        property: KProperty1<T, Long?>,
        @Suppress("UNUSED_PARAMETER") `_`: Long? = null
    ) {
        metadata.version = VersionMeta(property.name)
    }

    fun createdAt(
        property: KProperty1<T, LocalDateTime?>,
        @Suppress("UNUSED_PARAMETER") `_`: LocalDateTime? = null
    ) {
        metadata.createdAt = CreatedAtMeta(property.name)
    }

    fun createdAt(
        property: KProperty1<T, OffsetDateTime?>,
        @Suppress("UNUSED_PARAMETER") `_`: OffsetDateTime? = null
    ) {
        metadata.createdAt = CreatedAtMeta(property.name)
    }

    fun updatedAt(
        property: KProperty1<T, LocalDateTime?>,
        @Suppress("UNUSED_PARAMETER") `_`: LocalDateTime? = null
    ) {
        metadata.updatedAt = UpdatedAtMeta(property.name)
    }

    fun updatedAt(
        property: KProperty1<T, OffsetDateTime?>,
        @Suppress("UNUSED_PARAMETER") `_`: OffsetDateTime? = null
    ) {
        metadata.updatedAt = UpdatedAtMeta(property.name)
    }

    fun embedded(property: KProperty1<T, *>) {
        val embedded = EmbeddedMeta(property.name)
        metadata.embeddedList.add(embedded)
    }

    fun listener(entityListener: EntityListener<T>) {
        metadata.listener = ListenerMeta(entityListener)
    }

    fun table(block: TableScope<T>.() -> Unit) = tableScope.block()
}

@Scope
class TableScope<T : Any>(private val metadata: MutableMetadata<T>) {
    fun name(name: String, quote: Boolean = false) {
        metadata.table = TableMeta(name, quote)
    }

    fun column(property: KProperty1<T, *>, name: String? = null, quote: Boolean = false) {
        val column = ColumnMeta(property.name, name, quote)
        metadata.columnList.add(column)
    }
}

fun entities(block: EntitiesScope.() -> Unit): Map<KClass<*>, Metadata<*>> {
    return mutableMapOf<KClass<*>, Metadata<*>>().also {
        EntitiesScope(it).block()
    }
}
