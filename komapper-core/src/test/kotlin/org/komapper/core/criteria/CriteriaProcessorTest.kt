package org.komapper.core.criteria

import java.sql.SQLException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.komapper.core.desc.CamelToSnake
import org.komapper.core.desc.DefaultDataDescFactory
import org.komapper.core.desc.DefaultEntityDescFactory
import org.komapper.core.desc.DefaultPropDescFactory
import org.komapper.core.jdbc.AbstractDialect
import org.komapper.core.metadata.CollectedMetadataResolver
import org.komapper.core.metadata.entities

internal class CriteriaProcessorTest {
    private data class Address(
        val id: Int,
        val street: String
    )

    private val metadata = entities {
        entity(Address::class) {
            id(Address::id)
        }
    }

    private class MyDialect : AbstractDialect() {
        override fun isUniqueConstraintViolation(exception: SQLException): Boolean = false
        override fun getSequenceSql(sequenceName: String): String = ""
    }

    private val namingStrategy = CamelToSnake()

    private val dataDescFactory = DefaultDataDescFactory(
        CollectedMetadataResolver(metadata),
        DefaultPropDescFactory(
            { it },
            namingStrategy
        )
    )

    private val factory = DefaultEntityDescFactory(
        dataDescFactory,
        { it },
        namingStrategy
    )

    @Test
    fun test() {
        val criteria = Criteria(Address::class).apply {
            val alias = this.alias
            where.add(Criterion.Eq(alias[Address::street], "a"))
        }
        val processor = CriteriaProcessor(MyDialect(), factory, criteria)
        val sql = processor.buildSelect()
        assertEquals("select t0_.id, t0_.street from address t0_ where t0_.street = ?", sql.text)
    }
}
