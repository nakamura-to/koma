package org.komapper.core.expr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DefaultExprExtensionsTest {

    private val extensions = DefaultExprExtensions { it }

    @Test
    fun escape() {
        with(extensions) {
            assertEquals("abc", "abc".escape())
        }
    }

    @Test
    fun asPrefix() {
        with(extensions) {
            assertEquals("abc%", "abc".asPrefix())
        }
    }

    @Test
    fun asInfix() {
        with(extensions) {
            assertEquals("%abc%", "abc".asInfix())
        }
    }

    @Test
    fun asSuffix() {
        with(extensions) {
            assertEquals("%abc", "abc".asSuffix())
        }
    }
}
