package org.komapper.core.criteria

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InsertTest {

    private data class Address(
        val id: Int,
        val street: String
    )

    @Test
    fun values() {
        val criteria = InsertCriteria(Address::class)
        val scope = InsertScope(criteria)
        val query = insert<Address> {
            values {
                value(Address::id, 1)
                value(Address::street, "aaa")
            }
        }
        scope.query(criteria.alias)
        assertEquals(2, criteria.values.size)
        assertEquals(Expression.Property(null, Address::id) to Expression.wrap(1), criteria.values[0])
        assertEquals(Expression.Property(null, Address::street) to Expression.wrap("aaa"), criteria.values[1])
    }
}
