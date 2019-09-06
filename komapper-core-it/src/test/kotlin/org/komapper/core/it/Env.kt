package org.komapper.core.it

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.komapper.core.Db
import org.komapper.core.DbConfig
import org.komapper.core.jdbc.PostgreSqlDialect
import org.komapper.core.jdbc.SimpleDataSource

class Env : BeforeAllCallback,
    AfterAllCallback,
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    ParameterResolver {

    val port: String = System.getenv("PGPORT") ?: "5432"

    val db = Db(
        DbConfig(
            dataSource = SimpleDataSource(url = "jdbc:postgresql://127.0.0.1:$port/komapper", user = "postgres"),
            dialect = PostgreSqlDialect(),
            useTransaction = true
        )
    )

    override fun beforeAll(context: ExtensionContext?) =
        db.transaction {
            db.execute(
                """
                CREATE SEQUENCE SEQUENCE_STRATEGY_ID INCREMENT BY 100 START WITH 1;
                CREATE SEQUENCE MY_SEQUENCE_STRATEGY_ID INCREMENT BY 100 START WITH 1;
                
                CREATE TABLE DEPARTMENT(DEPARTMENT_ID INTEGER NOT NULL PRIMARY KEY, DEPARTMENT_NO INTEGER NOT NULL UNIQUE,DEPARTMENT_NAME VARCHAR(20),LOCATION VARCHAR(20) DEFAULT 'TOKYO', VERSION INTEGER);
                CREATE TABLE ADDRESS(ADDRESS_ID INTEGER NOT NULL PRIMARY KEY, STREET VARCHAR(20), VERSION INTEGER);
                CREATE TABLE EMPLOYEE(EMPLOYEE_ID INTEGER NOT NULL PRIMARY KEY, EMPLOYEE_NO INTEGER NOT NULL ,EMPLOYEE_NAME VARCHAR(20),MANAGER_ID INTEGER,HIREDATE DATE,SALARY NUMERIC(7,2),DEPARTMENT_ID INTEGER,ADDRESS_ID INTEGER, VERSION INTEGER, CONSTRAINT FK_DEPARTMENT_ID FOREIGN KEY(DEPARTMENT_ID) REFERENCES DEPARTMENT(DEPARTMENT_ID), CONSTRAINT FK_ADDRESS_ID FOREIGN KEY(ADDRESS_ID) REFERENCES ADDRESS(ADDRESS_ID));
                
                CREATE TABLE COMP_KEY_DEPARTMENT(DEPARTMENT_ID1 INTEGER NOT NULL, DEPARTMENT_ID2 INTEGER NOT NULL, DEPARTMENT_NO INTEGER NOT NULL UNIQUE,DEPARTMENT_NAME VARCHAR(20),LOCATION VARCHAR(20) DEFAULT 'TOKYO', VERSION INTEGER, CONSTRAINT PK_COMP_KEY_DEPARTMENT PRIMARY KEY(DEPARTMENT_ID1, DEPARTMENT_ID2));
                CREATE TABLE COMP_KEY_ADDRESS(ADDRESS_ID1 INTEGER NOT NULL, ADDRESS_ID2 INTEGER NOT NULL, STREET VARCHAR(20), VERSION INTEGER, CONSTRAINT PK_COMP_KEY_ADDRESS PRIMARY KEY(ADDRESS_ID1, ADDRESS_ID2));
                CREATE TABLE COMP_KEY_EMPLOYEE(EMPLOYEE_ID1 INTEGER NOT NULL, EMPLOYEE_ID2 INTEGER NOT NULL, EMPLOYEE_NO INTEGER NOT NULL ,EMPLOYEE_NAME VARCHAR(20),MANAGER_ID1 INTEGER,MANAGER_ID2 INTEGER,HIREDATE DATE,SALARY NUMERIC(7,2),DEPARTMENT_ID1 INTEGER,DEPARTMENT_ID2 INTEGER,ADDRESS_ID1 INTEGER,ADDRESS_ID2 INTEGER,VERSION INTEGER, CONSTRAINT PK_COMP_KEY_EMPLOYEE PRIMARY KEY(EMPLOYEE_ID1, EMPLOYEE_ID2), CONSTRAINT FK_COMP_KEY_DEPARTMENT_ID FOREIGN KEY(DEPARTMENT_ID1, DEPARTMENT_ID2) REFERENCES COMP_KEY_DEPARTMENT(DEPARTMENT_ID1, DEPARTMENT_ID2), CONSTRAINT FK_COMP_KEY_ADDRESS_ID FOREIGN KEY(ADDRESS_ID1, ADDRESS_ID2) REFERENCES COMP_KEY_ADDRESS(ADDRESS_ID1, ADDRESS_ID2));
                
                CREATE TABLE LARGE_OBJECT(ID NUMERIC(8) NOT NULL PRIMARY KEY, NAME VARCHAR(20), LARGE_NAME TEXT, BYTES BYTEA, LARGE_BYTES OID, DTO BYTEA, LARGE_DTO OID);
                CREATE TABLE TENSE (ID INTEGER NOT NULL PRIMARY KEY,DATE_DATE DATE, DATE_TIME TIME, DATE_TIMESTAMP TIMESTAMP, CAL_DATE DATE, CAL_TIME TIME, CAL_TIMESTAMP TIMESTAMP, SQL_DATE DATE, SQL_TIME TIME, SQL_TIMESTAMP TIMESTAMP);
                CREATE TABLE JOB (ID INTEGER NOT NULL PRIMARY KEY, JOB_TYPE VARCHAR(20));
                CREATE TABLE AUTHORITY (ID INTEGER NOT NULL PRIMARY KEY, AUTHORITY_TYPE INTEGER);
                CREATE TABLE NO_ID (VALUE1 INTEGER, VALUE2 INTEGER);
                CREATE TABLE OWNER_OF_NO_ID (ID INTEGER NOT NULL PRIMARY KEY, NO_ID_VALUE1 INTEGER);
                CREATE TABLE CONSTRAINT_CHECKING (PRIMARY_KEY INTEGER PRIMARY KEY, UNIQUE_KEY INTEGER UNIQUE, FOREIGN_KEY INTEGER, CHECK_CONSTRAINT INTEGER, NOT_NULL INTEGER NOT NULL, CONSTRAINT CK_CONSTRAINT_CHECKING_1 CHECK (CHECK_CONSTRAINT > 0), CONSTRAINT FK_JOB_ID FOREIGN KEY (FOREIGN_KEY) REFERENCES JOB (ID));
                CREATE TABLE PATTERN (VALUE VARCHAR(10));
                CREATE TABLE SAL_EMP (NAME TEXT PRIMARY KEY, PAY_BY_QUARTER INTEGER[], SCHEDULE TEXT[][]);
                
                CREATE TABLE ID_GENERATOR(PK VARCHAR(20) NOT NULL PRIMARY KEY, VALUE INTEGER NOT NULL);
                CREATE TABLE MY_ID_GENERATOR(MY_PK VARCHAR(20) NOT NULL PRIMARY KEY, MY_VALUE INTEGER NOT NULL);
                CREATE TABLE AUTO_STRATEGY(ID SERIAL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE IDENTITY_STRATEGY(ID SERIAL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE SEQUENCE_STRATEGY(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE SEQUENCE_STRATEGY2(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE TABLE_STRATEGY(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE TABLE_STRATEGY2(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
                CREATE TABLE PRODUCT(ID INTEGER NOT NULL PRIMARY KEY, VALUE XML);
                CREATE TABLE VERY_LONG_CHARACTERS_NAMED_TABLE(VERY_LONG_CHARACTERS_NAMED_TABLE_ID SERIAL PRIMARY KEY, VALUE VARCHAR(10));

                CREATE TABLE BIG_DECIMAL_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
                CREATE TABLE BIG_INTEGER_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
                CREATE TABLE BOOLEAN_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BOOL);
                CREATE TABLE BYTE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE INT2);
                CREATE TABLE BYTE_ARRAY_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BYTEA);
                CREATE TABLE DOUBLE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE FLOAT8);
                CREATE TABLE ENUM_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(20));
                CREATE TABLE FLOAT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE FLOAT);
                CREATE TABLE INT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE INTEGER);
                CREATE TABLE LOCAL_DATE_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIMESTAMP);
                CREATE TABLE LOCAL_DATE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE DATE);
                CREATE TABLE LOCAL_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIME);
                CREATE TABLE LONG_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
                CREATE TABLE OFFSET_DATE_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIMESTAMP WITH TIME ZONE);
                CREATE TABLE SHORT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE SMALLINT);
                CREATE TABLE STRING_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(20));
                
                INSERT INTO DEPARTMENT VALUES(1,10,'ACCOUNTING','NEW YORK',1);
                INSERT INTO DEPARTMENT VALUES(2,20,'RESEARCH','DALLAS',1);
                INSERT INTO DEPARTMENT VALUES(3,30,'SALES','CHICAGO',1);
                INSERT INTO DEPARTMENT VALUES(4,40,'OPERATIONS','BOSTON',1);
                INSERT INTO ADDRESS VALUES(1,'STREET 1',1);
                INSERT INTO ADDRESS VALUES(2,'STREET 2',1);
                INSERT INTO ADDRESS VALUES(3,'STREET 3',1);
                INSERT INTO ADDRESS VALUES(4,'STREET 4',1);
                INSERT INTO ADDRESS VALUES(5,'STREET 5',1);
                INSERT INTO ADDRESS VALUES(6,'STREET 6',1);
                INSERT INTO ADDRESS VALUES(7,'STREET 7',1);
                INSERT INTO ADDRESS VALUES(8,'STREET 8',1);
                INSERT INTO ADDRESS VALUES(9,'STREET 9',1);
                INSERT INTO ADDRESS VALUES(10,'STREET 10',1);
                INSERT INTO ADDRESS VALUES(11,'STREET 11',1);
                INSERT INTO ADDRESS VALUES(12,'STREET 12',1);
                INSERT INTO ADDRESS VALUES(13,'STREET 13',1);
                INSERT INTO ADDRESS VALUES(14,'STREET 14',1);
                INSERT INTO EMPLOYEE VALUES(1,7369,'SMITH',13,'1980-12-17',800,2,1,1);
                INSERT INTO EMPLOYEE VALUES(2,7499,'ALLEN',6,'1981-02-20',1600,3,2,1);
                INSERT INTO EMPLOYEE VALUES(3,7521,'WARD',6,'1981-02-22',1250,3,3,1);
                INSERT INTO EMPLOYEE VALUES(4,7566,'JONES',9,'1981-04-02',2975,2,4,1);
                INSERT INTO EMPLOYEE VALUES(5,7654,'MARTIN',6,'1981-09-28',1250,3,5,1);
                INSERT INTO EMPLOYEE VALUES(6,7698,'BLAKE',9,'1981-05-01',2850,3,6,1);
                INSERT INTO EMPLOYEE VALUES(7,7782,'CLARK',9,'1981-06-09',2450,1,7,1);
                INSERT INTO EMPLOYEE VALUES(8,7788,'SCOTT',4,'1982-12-09',3000.0,2,8,1);
                INSERT INTO EMPLOYEE VALUES(9,7839,'KING',NULL,'1981-11-17',5000,1,9,1);
                INSERT INTO EMPLOYEE VALUES(10,7844,'TURNER',6,'1981-09-08',1500,3,10,1);
                INSERT INTO EMPLOYEE VALUES(11,7876,'ADAMS',8,'1983-01-12',1100,2,11,1);
                INSERT INTO EMPLOYEE VALUES(12,7900,'JAMES',6,'1981-12-03',950,3,12,1);
                INSERT INTO EMPLOYEE VALUES(13,7902,'FORD',4,'1981-12-03',3000,2,13,1);
                INSERT INTO EMPLOYEE VALUES(14,7934,'MILLER',7,'1982-01-23',1300,1,14,1);
                
                INSERT INTO COMP_KEY_DEPARTMENT VALUES(1,1,10,'ACCOUNTING','NEW YORK',1);
                INSERT INTO COMP_KEY_DEPARTMENT VALUES(2,2,20,'RESEARCH','DALLAS',1);
                INSERT INTO COMP_KEY_DEPARTMENT VALUES(3,3,30,'SALES','CHICAGO',1);
                INSERT INTO COMP_KEY_DEPARTMENT VALUES(4,4,40,'OPERATIONS','BOSTON',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(1,1,'STREET 1',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(2,2,'STREET 2',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(3,3,'STREET 3',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(4,4,'STREET 4',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(5,5,'STREET 5',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(6,6,'STREET 6',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(7,7,'STREET 7',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(8,8,'STREET 8',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(9,9,'STREET 9',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(10,10,'STREET 10',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(11,11,'STREET 11',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(12,12,'STREET 12',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(13,13,'STREET 13',1);
                INSERT INTO COMP_KEY_ADDRESS VALUES(14,14,'STREET 14',1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(1,1,7369,'SMITH',13,13,'1980-12-17',800,2,2,1,1,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(2,2,7499,'ALLEN',6,6,'1981-02-20',1600,3,3,2,2,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(3,3,7521,'WARD',6,6,'1981-02-22',1250,3,3,3,3,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(4,4,7566,'JONES',9,9,'1981-04-02',2975,2,2,4,4,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(5,5,7654,'MARTIN',6,6,'1981-09-28',1250,3,3,5,5,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(6,6,7698,'BLAKE',9,9,'1981-05-01',2850,3,3,6,6,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(7,7,7782,'CLARK',9,9,'1981-06-09',2450,1,1,7,7,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(8,8,7788,'SCOTT',4,4,'1982-12-09',3000.0,2,2,8,8,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(9,9,7839,'KING',NULL,NULL,'1981-11-17',5000,1,1,9,9,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(10,10,7844,'TURNER',6,6,'1981-09-08',1500,3,3,10,10,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(11,11,7876,'ADAMS',8,8,'1983-01-12',1100,2,2,11,11,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(12,12,7900,'JAMES',6,6,'1981-12-03',950,3,3,12,12,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(13,13,7902,'FORD',4,4,'1981-12-03',3000,2,2,13,13,1);
                INSERT INTO COMP_KEY_EMPLOYEE VALUES(14,14,7934,'MILLER',7,7,'1982-01-23',1300,1,1,14,14,1);
                
                INSERT INTO TENSE VALUES (1, '2005-02-14', '12:11:10', '2005-02-14 12:11:10', '2005-02-14', '12:11:10', '2005-02-14 12:11:10', '2005-02-14', '12:11:10', '2005-02-14 12:11:10');
                INSERT INTO JOB VALUES (1, 'SALESMAN');
                INSERT INTO JOB VALUES (2, 'MANAGER');
                INSERT INTO JOB VALUES (3, 'PRESIDENT');
                INSERT INTO AUTHORITY VALUES (1, 10);
                INSERT INTO AUTHORITY VALUES (2, 20);
                INSERT INTO AUTHORITY VALUES (3, 30);
                INSERT INTO NO_ID VALUES (1, 1);
                INSERT INTO NO_ID VALUES (1, 1);
                INSERT INTO SAL_EMP VALUES ('Bill', '{10000, 10000, 10000, 10000}', '{{"meeting", "lunch"}, {"training", "presentation"}}');
                INSERT INTO SAL_EMP VALUES ('Carol', '{20000, 25000, 25000, 25000}', '{{"breakfast", "consulting"}, {"meeting", "lunch"}}');
                
                INSERT INTO ID_GENERATOR VALUES('TABLE_STRATEGY_ID', 1);
                INSERT INTO MY_ID_GENERATOR VALUES('TableStrategy2', 1);
            """.trimIndent()
            )
        }

    override fun afterAll(context: ExtensionContext?) =
        db.transaction {
            db.execute(
                """
                DROP TABLE EMPLOYEE;
                DROP TABLE ADDRESS;
                DROP TABLE DEPARTMENT;
                
                DROP TABLE COMP_KEY_EMPLOYEE;
                DROP TABLE COMP_KEY_ADDRESS;
                DROP TABLE COMP_KEY_DEPARTMENT;
                
                DROP TABLE CONSTRAINT_CHECKING;
                DROP TABLE LARGE_OBJECT;
                DROP TABLE TENSE;
                DROP TABLE JOB;
                DROP TABLE AUTHORITY;
                DROP TABLE NO_ID;
                DROP TABLE OWNER_OF_NO_ID;
                DROP TABLE PATTERN;
                DROP TABLE SAL_EMP;
                
                DROP TABLE ID_GENERATOR;
                DROP TABLE MY_ID_GENERATOR;
                DROP TABLE AUTO_STRATEGY;
                DROP TABLE IDENTITY_STRATEGY;
                DROP TABLE SEQUENCE_STRATEGY;
                DROP TABLE SEQUENCE_STRATEGY2;
                DROP TABLE TABLE_STRATEGY;
                DROP TABLE TABLE_STRATEGY2;
                DROP TABLE PRODUCT;
                DROP TABLE VERY_LONG_CHARACTERS_NAMED_TABLE;
                
                DROP TABLE BIG_DECIMAL_TEST;
                DROP TABLE BIG_INTEGER_TEST;
                DROP TABLE BOOLEAN_TEST;
                DROP TABLE BYTE_TEST;
                DROP TABLE BYTE_ARRAY_TEST;
                DROP TABLE DOUBLE_TEST;
                DROP TABLE ENUM_TEST;
                DROP TABLE FLOAT_TEST;
                DROP TABLE INT_TEST;
                DROP TABLE LOCAL_DATE_TIME_TEST;
                DROP TABLE LOCAL_DATE_TEST;
                DROP TABLE LOCAL_TIME_TEST;
                DROP TABLE LONG_TEST;
                DROP TABLE OFFSET_DATE_TIME_TEST;
                DROP TABLE SHORT_TEST;
                DROP TABLE STRING_TEST;

                DROP SEQUENCE SEQUENCE_STRATEGY_ID;
                DROP SEQUENCE MY_SEQUENCE_STRATEGY_ID;
            """.trimIndent()
            )
        }

    override fun beforeTestExecution(context: ExtensionContext?) =
        db.config.transactionManager.begin(db.config.isolationLevel)

    override fun afterTestExecution(context: ExtensionContext?) =
        db.config.transactionManager.rollback()

    override fun supportsParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Boolean =
        parameterContext!!.parameter.type === Db::class.java

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Any = db
}
