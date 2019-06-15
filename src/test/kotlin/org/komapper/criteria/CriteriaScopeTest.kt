package org.komapper.criteria

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CriteriaScopeTest {

    private data class Address(
        val aaa: Int,
        val bbb: String,
        val ccc: Int,
        val ddd: String,
        val eee: Int,
        val fff: String,
        val ggg: Int
    )

    @Test
    fun where() {
        val scope = CriteriaScope()
        scope.where {
            Address::aaa eq 1
            Address::bbb ne "B"
            or {
                Address::ccc gt 2
                Address::ddd ge "D"
            }
            and {
                Address::eee lt 3
                Address::fff le "F"
            }
        }
        val criteria = scope()
        val whereScope = criteria.whereScope
        assertEquals(4, whereScope.criterionList.size)
        assertEquals(whereScope.criterionList[0], Criterion.Eq(Address::aaa, 1))
        assertEquals(whereScope.criterionList[1], Criterion.Ne(Address::bbb, "B"))
        assertEquals(
            whereScope.criterionList[2], Criterion.Or(
                listOf(
                    Criterion.Gt(Address::ccc, 2),
                    Criterion.Ge(Address::ddd, "D")
                )
            )
        )
        assertEquals(
            whereScope.criterionList[3], Criterion.And(
                listOf(
                    Criterion.Lt(Address::eee, 3),
                    Criterion.Le(Address::fff, "F")
                )
            )
        )
    }

    @Test
    fun orderBy() {
        val scope = CriteriaScope()
        scope.orderBy {
            Address::aaa.desc()
            Address::bbb.asc()
        }
        val criteria = scope()
        val orderByScope = criteria.orderByScope
        assertEquals(2, orderByScope.items.size)
        assertEquals(orderByScope.items[0], Address::aaa to "desc")
        assertEquals(orderByScope.items[1], Address::bbb to "asc")
    }

    @Test
    fun limit() {
        val scope = CriteriaScope()
        scope.limit(10)
        val criteria = scope()
        assertEquals(10, criteria.limit)
    }

    @Test
    fun offset() {
        val scope = CriteriaScope()
        scope.offset(100)
        val criteria = scope()
        assertEquals(100, criteria.offset)
    }

    @Test
    fun where_orderBy_limit_offset() {
        val scope = CriteriaScope()
        scope.where {
            Address::aaa eq 1
        }.orderBy {
            Address::bbb.desc()
        }.limit(5).offset(15)
        val criteria = scope()
        val whereScope = criteria.whereScope
        val orderByScope = criteria.orderByScope
        val limit = criteria.limit
        val offset = criteria.offset
        assertEquals(1, whereScope.criterionList.size)
        assertEquals(whereScope.criterionList[0], Criterion.Eq(Address::aaa, 1))
        assertEquals(1, orderByScope.items.size)
        assertEquals(orderByScope.items[0], Address::bbb to "desc")
        assertEquals(5, limit)
        assertEquals(15, offset)
    }

}

