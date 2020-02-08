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
import org.komapper.core.desc.EntityDesc
import org.komapper.core.desc.EntityListener
import org.komapper.core.desc.GlobalEntityListener

@ExtendWith(Env::class)
internal class DeleteTest(private val db: Db) {

    @Test
    fun test() {
        val sql = "select * from address where address_id = 15"
        val address = db.query<Address>(sql).first()
        db.delete(address)
        val address2 = db.query<Address>(sql).firstOrNull()
        Assertions.assertNull(address2)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    @Test
    fun globalEntityListener() {
        val db = Db(
            object : DbConfig() {
                override val dataSource = db.config.dataSource
                override val dialect = db.config.dialect
                override val metadataResolver = db.config.metadataResolver
                override val listener: GlobalEntityListener =
                    object : GlobalEntityListener {
                        override fun <T : Any> preDelete(
                            entity: T,
                            desc: EntityDesc<T>
                        ): T {
                            return when (entity) {
                                is Address -> entity.copy(street = "*${entity.street}")
                                else -> entity
                            } as T
                        }

                        override fun <T : Any> postDelete(
                            entity: T,
                            desc: EntityDesc<T>
                        ): T {
                            return when (entity) {
                                is Address -> entity.copy(street = "${entity.street}*")
                                else -> entity
                            } as T
                        }
                    }
            }
        )

        val sql = "select * from address where address_id = 15"
        val address = db.query<Address>(sql).first()
        val address2 = db.delete(address)
        Assertions.assertEquals(
            Address(
                15,
                "*STREET 15*",
                1
            ), address2
        )
        val address3 = db.query<Address>(sql).firstOrNull()
        Assertions.assertNull(address3)
    }

    @Test
    fun entityListener() {
        val db = Db(
            AddressListenerConfig(
                db.config,
                object :
                    EntityListener<Address> {
                    override fun preDelete(
                        entity: Address,
                        desc: EntityDesc<Address>
                    ): Address {
                        return entity.copy(street = "*${entity.street}")
                    }

                    override fun postDelete(
                        entity: Address,
                        desc: EntityDesc<Address>
                    ): Address {
                        return entity.copy(street = "${entity.street}*")
                    }
                })
        )

        val sql = "select * from address where address_id = 15"
        val address = db.query<Address>(sql).first()
        val address2 = db.delete(address)
        Assertions.assertEquals(
            Address(
                15,
                "*STREET 15*",
                1
            ), address2
        )
        val address3 = db.query<Address>(sql).firstOrNull()
        Assertions.assertNull(address3)
    }

    @Test
    fun optimisticLockException() {
        val sql = "select * from address where address_id = 15"
        val address = db.query<Address>(sql).first()
        db.delete(address)
        assertThrows<OptimisticLockException> {
            db.delete(
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
        db.delete(employee)
        val employee3 = db.findById<Employee>(100)
        Assertions.assertNull(employee3)
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
        db.delete(worker)
        val worker3 = db.findById<Worker>(100)
        Assertions.assertNull(worker3)
    }

    @Test
    fun embedded_valueAssignment() {
        val human = Human(
            name = "aaa",
            common = Common()
        )
        val human2 = db.insert(human)
        db.delete(human2)
        val human3 = db.findById<Human>(1)
        Assertions.assertNull(human3)
    }
}