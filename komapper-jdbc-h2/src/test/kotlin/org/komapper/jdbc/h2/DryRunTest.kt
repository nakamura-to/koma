package org.komapper.jdbc.h2

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.Db
import org.komapper.core.sql.template

@ExtendWith(Env::class)
internal class DryRunTest(private val db: Db) {

    @Test
    fun findById() {
        val (sql) = db.dryRun.findById<Address>(2)
        println(sql)
    }

    @Test
    fun select() {
        val (sql) = db.dryRun.select<Address> {
            where {
                ge(Address::addressId, 1)
            }
            orderBy {
                desc(Address::addressId)
            }
            limit(2)
            offset(5)
        }
        println(sql)
    }

    @Test
    fun query() {
        val (sql) = db.dryRun.select<Address>(template("select * from address"))
        println(sql)
    }

    @Test
    fun paginate() {
        val (sql) = db.dryRun.paginate<Address>(template("select * from address"), limit = 3, offset = 5)
        println(sql)
    }

    @Test
    fun queryOneColumn() {
        val t = template<String>("select street from address")
        val sql = db.dryRun.selectOneColumn(t)
        println(sql)
    }

    @Test
    fun queryTwoColumns() {
        val t = template<Pair<Int, String>>("select address_id, street from address")
        val sql = db.dryRun.selectTwoColumns(t)
        println(sql)
    }

    @Test
    fun queryThreeColumns() {
        val t = template<Triple<Int, String, Int>>("select address_id, street, version from address")
        val sql = db.dryRun.selectThreeColumns(t)
        println(sql)
    }

    @Test
    fun delete() {
        val t = template<Address>("select * from address where address_id = 15")
        val address = db.select(t).first()
        val (sql) = db.dryRun.delete(address)
        println(sql)
    }

    @Test
    fun insert() {
        val strategy = SequenceStrategy(-100, "a")
        val (sql) = db.dryRun.insert(strategy)
        Assertions.assertEquals(
            "insert into SEQUENCE_STRATEGY (id, value) values (0, 'a')",
            sql.log
        )
    }

    @Test
    fun update() {
        val t = template<Address>("select * from address where address_id = 15")
        val address = db.select(t).first()
        val newAddress = address.copy(street = "NY street")
        val (sql) = db.dryRun.update(newAddress)
        println(sql)
    }

    @Test
    fun merge() {
        val department = Department(5, 50, "PLANNING", "TOKYO", 0)
        val (sql) = db.dryRun.merge(department, Department::departmentNo)
        println(sql)
    }

    @Test
    fun batchDelete() {
        val addressList = listOf(
            Address(16, "STREET 16", 0),
            Address(17, "STREET 17", 0),
            Address(18, "STREET 18", 0)
        )
        val (sqls) = db.dryRun.batchDelete(addressList)
        Assertions.assertEquals(3, sqls.size)
    }

    @Test
    fun batchInsert() {
        val addressList = listOf(
            Address(16, "STREET 16", 0),
            Address(17, "STREET 17", 0),
            Address(18, "STREET 18", 0)
        )
        val (sqls) = db.dryRun.batchInsert(addressList)
        Assertions.assertEquals(3, sqls.size)
    }

    @Test
    fun batchUpdate() {
        val personList = listOf(
            Person(1, "A"),
            Person(2, "B"),
            Person(3, "C")
        )
        val (sqls) = db.dryRun.batchUpdate(personList)
        Assertions.assertEquals(3, sqls.size)
    }

    @Test
    fun batchMerge() {
        val departments = listOf(
            Department(5, 50, "PLANNING", "TOKYO", 0),
            Department(6, 10, "DEVELOPMENT", "KYOTO", 0)
        )
        val (sqls) = db.dryRun.batchMerge(departments, Department::departmentNo)
        Assertions.assertEquals(2, sqls.size)
    }
}
