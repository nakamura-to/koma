package org.komapper.core.expr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.komapper.core.expr.ExprTokenType.*

class ExprTokenizerTest {

    @Test
    fun value() {
        val tokenizer = ExprTokenizer("name")
        assertEquals(VALUE, tokenizer.next())
        assertEquals("name", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun classRef() {
        val tokenizer = ExprTokenizer("@aaa.bbb.Ccc@")
        assertEquals(CLASS_REF, tokenizer.next())
        assertEquals("@aaa.bbb.Ccc@", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun stringLiteral() {
        val tokenizer = ExprTokenizer("\"aaa bbb\"")
        assertEquals(STRING, tokenizer.next())
        assertEquals("\"aaa bbb\"", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun intLiteral() {
        val tokenizer = ExprTokenizer("+13")
        assertEquals(INT, tokenizer.next())
        assertEquals("+13", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun longLiteral() {
        val tokenizer = ExprTokenizer("+13L")
        assertEquals(LONG, tokenizer.next())
        assertEquals("+13L", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun floatLiteral() {
        val tokenizer = ExprTokenizer("+13F")
        assertEquals(FLOAT, tokenizer.next())
        assertEquals("+13F", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun doubleLiteral() {
        val tokenizer = ExprTokenizer("+13D")
        assertEquals(DOUBLE, tokenizer.next())
        assertEquals("+13D", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun bigDecimalLiteral() {
        val tokenizer = ExprTokenizer("+13B")
        assertEquals(BIG_DECIMAL, tokenizer.next())
        assertEquals("+13B", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun nullLiteral() {
        val tokenizer = ExprTokenizer("null")
        assertEquals(NULL, tokenizer.next())
        assertEquals("null", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun trueLiteral() {
        val tokenizer = ExprTokenizer("true")
        assertEquals(TRUE, tokenizer.next())
        assertEquals("true", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun falseLiteral() {
        val tokenizer = ExprTokenizer("false")
        assertEquals(FALSE, tokenizer.next())
        assertEquals("false", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun expressions() {
        val tokenizer = ExprTokenizer("manager.aaa && name.bbb")
        assertEquals(VALUE, tokenizer.next())
        assertEquals("manager", tokenizer.token)
        assertEquals(PROPERTY, tokenizer.next())
        assertEquals(".aaa", tokenizer.token)
        assertEquals(WHITESPACE, tokenizer.next())
        assertEquals(" ", tokenizer.token)
        assertEquals(AND, tokenizer.next())
        assertEquals("&&", tokenizer.token)
        assertEquals(WHITESPACE, tokenizer.next())
        assertEquals(" ", tokenizer.token)
        assertEquals(VALUE, tokenizer.next())
        assertEquals("name", tokenizer.token)
        assertEquals(PROPERTY, tokenizer.next())
        assertEquals(".bbb", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun position() {
        val tokenizer = ExprTokenizer("aaa bbb ccc")
        assertEquals(0, tokenizer.location.position)
        assertEquals(VALUE, tokenizer.next())
        assertEquals("aaa", tokenizer.token)
        assertEquals(3, tokenizer.location.position)
        assertEquals(WHITESPACE, tokenizer.next())
        assertEquals(" ", tokenizer.token)
        assertEquals(4, tokenizer.location.position)
        assertEquals(VALUE, tokenizer.next())
        assertEquals("bbb", tokenizer.token)
        assertEquals(7, tokenizer.location.position)
        assertEquals(WHITESPACE, tokenizer.next())
        assertEquals(" ", tokenizer.token)
        assertEquals(8, tokenizer.location.position)
        assertEquals(VALUE, tokenizer.next())
        assertEquals("ccc", tokenizer.token)
        assertEquals(11, tokenizer.location.position)
    }

    @Test
    fun property() {
        val tokenizer = ExprTokenizer("aaa.bbb")
        assertEquals(VALUE, tokenizer.next())
        assertEquals("aaa", tokenizer.token)
        assertEquals(PROPERTY, tokenizer.next())
        assertEquals(".bbb", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun safeCallProperty() {
        val tokenizer = ExprTokenizer("aaa?.bbb")
        assertEquals(VALUE, tokenizer.next())
        assertEquals("aaa", tokenizer.token)
        assertEquals(SAFE_CALL_PROPERTY, tokenizer.next())
        assertEquals("?.bbb", tokenizer.token)
        assertEquals(EOE, tokenizer.next())
        assertEquals("", tokenizer.token)
    }

    @Test
    fun `The end of single quotation mark is not found`() {
        val tokenizer = ExprTokenizer("'aaa")
        val exception = assertThrows<ExprException> { tokenizer.next() }
        println(exception)
    }

    @Test
    fun `The end of double quotation mark is not found`() {
        val tokenizer = ExprTokenizer("\"aaa")
        val exception = assertThrows<ExprException> { tokenizer.next() }
        println(exception)
    }

    @Test
    fun `Either property or function name must follow the dot`() {
        val tokenizer = ExprTokenizer(".")
        val exception = assertThrows<ExprException> { tokenizer.next() }
        println(exception)
    }

    @Test
    fun `The character is illegal as an identifier start`() {
        val tokenizer = ExprTokenizer(".!")
        val exception = assertThrows<ExprException> { tokenizer.next() }
        println(exception)
    }

}
