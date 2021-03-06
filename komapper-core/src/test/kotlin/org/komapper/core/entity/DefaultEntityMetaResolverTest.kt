package org.komapper.core.entity

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class DefaultEntityMetaResolverTest {

    data class Address(
        val id: Int,
        val name: String,
        val version: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    )

    private val metadata = entities {
        entity(Address::class) {
            id(Address::id, SequenceGenerator("address_seq"))
            version(Address::version)
            createdAt(Address::createdAt)
            updatedAt(Address::updatedAt)
            table {
                name("ADDRESS")
                column(Address::id, name = "address_id", quote = true)
            }
        }
    }

    data class Person(
        val id: Int,
        val name: String,
        val version: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    )

    @Test
    fun testRegistered() {
        val resolver = DefaultEntityMetaResolver(metadata)
        assertEquals(metadata[Address::class], resolver.resolve(
            Address::class))
    }

    @Test
    fun testNotRegistered() {
        val resolver = DefaultEntityMetaResolver(metadata)
        assertDoesNotThrow {
            resolver.resolve(Person::class)
        }
    }
}
