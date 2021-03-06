package org.komapper.jdbc.postgresql

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.Db
import org.komapper.core.sql.template

@ExtendWith(Env::class)
class PaginateTest {

    @Test
    fun test(db: Db) {
        val t = template<Employee>("select /*%expand*/* from Employee order by employee_id")
        val (list, count) = db.paginate(
            t,
            limit = 3,
            offset = 5
        )
        assertEquals(3, list.size)
        assertEquals(listOf(6, 7, 8), list.map { it.employeeId })
        assertEquals(14, count)
    }
}
