package org.komapper.jdbc.h2

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.Db
import org.komapper.core.DbConfig
import org.komapper.core.OptimisticLockException
import org.komapper.core.UniqueConstraintException
import org.komapper.core.criteria.expression
import org.komapper.core.criteria.update
import org.komapper.core.entity.EntityDesc
import org.komapper.core.entity.EntityListener
import org.komapper.core.entity.GlobalEntityListener
import org.komapper.core.sql.template

@ExtendWith(Env::class)
internal class UpdateTest(private val db: Db) {

    @Test
    fun test() {
        val sql = template<Address>("select * from address where address_id = 15")
        val address = db.select(sql).first()
        val newAddress = address.copy(street = "NY street")
        db.update(newAddress)
        val address2 = db.select(sql).firstOrNull()
        Assertions.assertEquals(
            Address(
                15,
                "NY street",
                2
            ), address2
        )
    }

    @Test
    fun updatedAt() {
        val person = Person(1, "ABC")
        val newPerson = db.insert(person).let {
            db.update(it)
        }
        Assertions.assertTrue(newPerson.updatedAt > person.updatedAt)
    }

    @Test
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun globalEntityListener() {
        val db = Db(object : DbConfig() {
            override val dataSource = db.config.dataSource
            override val dialect = db.config.dialect
            override val entityMetaResolver = db.config.entityMetaResolver
            override val listener = object : GlobalEntityListener {
                override fun <T : Any> preUpdate(
                    entity: T,
                    desc: EntityDesc<T>
                ): T {
                    return when (entity) {
                        is Address -> entity.copy(street = "*${entity.street}")
                        else -> entity
                    } as T
                }

                override fun <T : Any> postUpdate(
                    entity: T,
                    desc: EntityDesc<T>
                ): T {
                    return when (entity) {
                        is Address -> entity.copy(street = "${entity.street}*")
                        else -> entity
                    } as T
                }
            }
        })

        val t = template<Address>("select * from address where address_id = 15")
        val address = db.select<Address>(t).first()
        val newAddress = address.copy(street = "NY street")
        val address2 = db.update(newAddress)
        Assertions.assertEquals(
            Address(
                15,
                "*NY street*",
                2
            ), address2
        )
        val address3 = db.select(t).firstOrNull()
        Assertions.assertEquals(
            Address(
                15,
                "*NY street",
                2
            ), address3
        )
    }

    @Test
    fun entityListener() {
        val db = Db(
            AddressListenerConfig(
                db.config,
                object :
                    EntityListener<Address> {
                    override fun preUpdate(
                        entity: Address,
                        desc: EntityDesc<Address>
                    ): Address {
                        return entity.copy(street = "*${entity.street}")
                    }

                    override fun postUpdate(
                        entity: Address,
                        desc: EntityDesc<Address>
                    ): Address {
                        return entity.copy(street = "${entity.street}*")
                    }
                })
        )

        val t = template<Address>("select * from address where address_id = 15")
        val address = db.select(t).first()
        val newAddress = address.copy(street = "NY street")
        val address2 = db.update(newAddress)
        Assertions.assertEquals(
            Address(
                15,
                "*NY street*",
                2
            ), address2
        )
        val address3 = db.select(t).firstOrNull()
        Assertions.assertEquals(
            Address(
                15,
                "*NY street",
                2
            ), address3
        )
    }

    @Test
    fun uniqueConstraintException() {
        val address = Address(1, "STREET 2", 1)
        assertThrows<UniqueConstraintException> {
            db.update(
                address
            )
        }
    }

    @Test
    fun optimisticLockException() {
        val t = template<Address>("select * from address where address_id = 15")
        val address = db.select(t).first()
        db.update(address)
        assertThrows<OptimisticLockException> {
            db.update(
                address
            )
        }
    }

    @Test
    fun embedded() {
        val employee = Employee(
            employeeId = 100,
            employeeNo = 9999,
            employeeName = "aaa",
            managerId = null,
            detail = EmployeeDetail(
                LocalDate.of(2019, 6, 15),
                BigDecimal("2000.00")
            ),
            departmentId = 1,
            addressId = 1,
            version = 1
        )
        db.insert(employee)
        val employee2 = db.findById<Employee>(100)
        Assertions.assertEquals(employee, employee2)

        val employee3 = employee.copy(detail = employee.detail.copy(salary = BigDecimal("5000.00")))
        val employee4 = db.update(employee3)
        Assertions.assertEquals(
            BigDecimal("5000.00"),
            employee4.detail.salary
        )

        val employee5 = db.findById<Employee>(100)
        Assertions.assertEquals(
            BigDecimal("5000.00"),
            employee5?.detail?.salary
        )
    }

    @Test
    fun nestedEmbedded() {
        val salary =
            WorkerSalary(BigDecimal("2000.00"))
        val worker = Worker(
            employeeId = 100,
            employeeNo = 9999,
            employeeName = "aaa",
            managerId = null,
            detail = WorkerDetail(
                LocalDate.of(
                    2019,
                    6,
                    15
                ), salary
            ),
            departmentId = 1,
            addressId = 1,
            version = 1
        )
        db.insert(worker)
        val worker2 = db.findById<Worker>(100)
        Assertions.assertEquals(worker, worker2)

        val worker3 = worker.copy(
            detail = worker.detail.copy(
                salary = WorkerSalary(
                    BigDecimal("5000.00")
                )
            )
        )
        val worker4 = db.update(worker3)
        Assertions.assertEquals(
            WorkerSalary(
                BigDecimal("5000.00")
            ), worker4.detail.salary
        )

        val worker5 = db.findById<Worker>(100)
        Assertions.assertEquals(
            WorkerSalary(
                BigDecimal("5000.00")
            ), worker5?.detail?.salary
        )
    }

    @Test
    fun embedded_valueAssignment() {
        val human = Human(
            name = "aaa",
            common = Common()
        )
        val human2 = db.insert(human)
        val human3 = human2.copy(name = "bbb")
        val human4 = db.update(human3)
        val human5 = db.findById<Human>(1)
        Assertions.assertEquals(human4, human5)
        println(human4)
    }

    @Test
    fun criteria() {
        val query = update<Address> {
            set {
                value(Address::street, "new street")
            }
            where {
                eq(Address::addressId, 15)
            }
        }
        val count = db.update(query)
        Assertions.assertEquals(1, count)
        val address = db.findById<Address>(15)
        Assertions.assertEquals(Address(15, "new street", 1), address)
    }

    @Test
    fun plus() {
        val query = update<Address> {
            set {
                value(Address::version, expression { Address::version + 10 })
            }
            where {
                eq(Address::addressId, 15)
            }
        }
        val count = db.update(query)
        Assertions.assertEquals(1, count)
        val address = db.findById<Address>(15)
        Assertions.assertEquals(Address(15, "STREET 15", 11), address)
    }

    @Test
    fun minus() {
        val query = update<Address> {
            set {
                value(Address::version, expression { Address::version - 10 })
            }
            where {
                eq(Address::addressId, 15)
            }
        }
        val count = db.update(query)
        Assertions.assertEquals(1, count)
        val address = db.findById<Address>(15)
        Assertions.assertEquals(Address(15, "STREET 15", -9), address)
    }

    @Test
    fun concat() {
        val query = update<Address> {
            set {
                value(Address::street, expression { "[" `||` Address::street `||` "]" })
            }
            where {
                eq(Address::addressId, 15)
            }
        }
        val count = db.update(query)
        Assertions.assertEquals(1, count)
        val address = db.findById<Address>(15)
        Assertions.assertEquals(Address(15, "[STREET 15]", 1), address)
    }
}
