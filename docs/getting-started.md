# Getting Started

## Download
### Gradle

Use H2 Database or PostgreSQL:

```groovy
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // for h2 database
    implementation("org.komapper:komapper-jdbc-h2:0.1.8")
    // for PosgreSQL
    implementation("org.komapper:komapper-jdbc-postgresql:0.1.8")
}
```

## Entity Definitions
Every entity class must be a Kotlin data class.
The definitions are described in the `entities` DSL API as follows:

```kotlin
data class Address(val id: Int = 0, val street: String, val version: Int = 0)
data class Emp(val id: Int = 0, val name:String, val addressId: Int = 0, val version: Int = 0)

val metadata = entities {
    // 1. The Address class is defined as an entity.
    entity<Address> {
        // 2. The id property is defined as an identifier. 
        //    The value is generated by the ADDRESS_SEQ sequence.
        id(Address::id, SequenceGenerator("ADDRESS_SEQ", 100))
        // 3. The version property is defined as a version for optimistic lock.
        version(Address::version)
        table {
            // 4. The id property is mapped to the address_id column.
            column(Address::id, "address_id")
        }
    }

    // 5. The Employee class is defined as an entity.
    entity<Employee> {
        id(Employee::id, SequenceGenerator("EMP_SEQ", 100))
        version(Employee::version)
        table {
            // 6. The Employee entity is mapped to the EMP table.
            name("EMP")
            column(Employee::id, "emp_id")
        }
    }
}
```

## Database Configuration
Create a `Db` object as follows:

```kotlin
val db = Db(object : DbConfig() {
    // dataSource for H2
    override val dataSource = SimpleDataSource("jdbc:h2:mem:example;DB_CLOSE_DELAY=-1")
    // dialect for H2
    override val dialect = H2Dialect()
    // register entity metadata
    override val entityMetaResolver = DefaultEntityMetaResolver(metadata)
})
```

Don't forget to pass the metadata to the DefaultEntityMetaResolver class.

The above configuration is for H2 Database.
When you use PostgreSQL, change the dataSource and the dialect.

## Transaction

### Thread-local transaction
Komapper provides thread-local transaction.
To use the transaction, pass database operations to the `db.transaction` function:

```kotlin
db.transaction {
    // execute database operations
}
```

### Other transaction mechanisms

When you use Komapper with other frameworks such as Spring Framework, 
consider to use the way the framework recommends.

## Example

```kotlin
package example

import org.komapper.core.Db
import org.komapper.core.DbConfig
import org.komapper.core.criteria.select
import org.komapper.core.entity.DefaultEntityMetaResolver
import org.komapper.core.entity.SequenceGenerator
import org.komapper.core.entity.entities
import org.komapper.core.jdbc.SimpleDataSource
import org.komapper.core.sql.template
import org.komapper.jdbc.h2.H2Dialect

// entity
data class Address(
    val id: Int = 0,
    val street: String,
    val version: Int = 0
)

// entity metadata
val metadata = entities {
    entity<Address> {
        id(Address::id, SequenceGenerator("ADDRESS_SEQ", 100))
        version(Address::version)
        table {
            column(Address::id, "address_id")
        }
    }
}

fun main() {
    // create Db instance
    val db = Db(
        // configuration
        object : DbConfig() {
            // dataSource for H2
            override val dataSource = SimpleDataSource("jdbc:h2:mem:example;DB_CLOSE_DELAY=-1")
            // dialect for H2
            override val dialect = H2Dialect()
            // register entity metadata
            override val entityMetaResolver = DefaultEntityMetaResolver(metadata)
        }
    )

    // set up schema
    db.transaction {
        db.execute(
            """
            CREATE SEQUENCE ADDRESS_SEQ START WITH 1 INCREMENT BY 100;
            CREATE TABLE ADDRESS(
                ADDRESS_ID INTEGER NOT NULL PRIMARY KEY,
                STREET VARCHAR(20) UNIQUE,
                VERSION INTEGER
            );
            """.trimIndent()
        )
    }

    // execute simple CRUD operations as a transaction
    db.transaction {
        // CREATE
        val addressA = db.insert(Address(street = "street A"))
        println(addressA)

        // READ: select by identifier
        val foundA = db.findById<Address>(1)
        assert(addressA == foundA)

        // UPDATE
        val addressB = db.update(addressA.copy(street = "street B"))
        println(addressB)

        // READ: select by criteria query
        val criteriaQuery = select<Address> {
            where {
                eq(Address::street, "street B")
            }
        }
        val foundB1 = db.select(criteriaQuery).first()
        assert(addressB == foundB1)

        // READ: select by template query
        val templateQuery = template<Address>(
            "select /*%expand*/* from Address where street = /*street*/'test'",
            object {
                val street = "street B"
            }
        )
        val foundB2 = db.select(templateQuery).first()
        assert(addressB == foundB2)

        // DELETE
        db.delete(addressB)

        // READ: select by criteria query
        val addressList = db.select<Address>()
        assert(addressList.isEmpty())
    }
}
```
