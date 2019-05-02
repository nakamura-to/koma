package koma.expr

import koma.expr.ExprTokenType.*
import java.nio.CharBuffer

class ExprTokenizer(private val expression: String) {

    private var buf = CharBuffer.wrap(expression)
    private var tokenBuf = buf.asReadOnlyBuffer()
    private var binaryOpAvailable = false
    private var position = 0
    private var type = EOE
    var token = ""
        private set
    val location
        get() = ExprLocation(expression, position)

    init {
        peek()
    }

    operator fun next(): ExprTokenType {
        return when (type) {
            EOE -> {
                token = ""
                EOE
            }
            else -> {
                val result = type
                prepare()
                peek()
                result
            }
        }
    }

    private fun prepare() {
        position = buf.position()
        tokenBuf.limit(position)
        token = tokenBuf.toString()
        tokenBuf.position(buf.position())
    }

    private fun peek() {
        if (buf.hasRemaining()) {
            val c = buf.get()
            if (buf.hasRemaining()) {
                val c2 = buf.get()
                if (buf.hasRemaining()) {
                    val c3 = buf.get()
                    if (buf.hasRemaining()) {
                        val c4 = buf.get()
                        if (buf.hasRemaining()) {
                            val c5 = buf.get()
                            peekFiveChars(c, c2, c3, c4, c5)
                        } else {
                            peekFourChars(c, c2, c3, c4)
                        }
                    } else {
                        peekThreeChars(c, c2, c3)
                    }
                } else {
                    peekTwoChars(c, c2)
                }
            } else {
                peekOneChar(c)
            }
        } else {
            type = EOE
        }
    }

    private fun peekFiveChars(c: Char, c2: Char, c3: Char, c4: Char, c5: Char) {
        if (c == 'f' && c2 == 'a' && c3 == 'l' && c4 == 's' && c5 == 'e') {
            if (isWordTerminated()) {
                type = FALSE
                binaryOpAvailable = true
                return
            }
        }
        buf.position(buf.position() - 1)
        peekFourChars(c, c2, c3, c4)
    }

    private fun peekFourChars(c: Char, c2: Char, c3: Char, c4: Char) {
        if (c == 'n' && c2 == 'u' && c3 == 'l' && c4 == 'l') {
            if (isWordTerminated()) {
                type = NULL
                binaryOpAvailable = true
                return
            }
        } else if (c == 't' && c2 == 'r' && c3 == 'u' && c4 == 'e') {
            if (isWordTerminated()) {
                type = TRUE
                binaryOpAvailable = true
                return
            }
        }
        buf.position(buf.position() - 1)
        peekThreeChars(c, c2, c3)
    }

    private fun peekThreeChars(c: Char, c2: Char, @Suppress("UNUSED_PARAMETER") c3: Char) {
        buf.position(buf.position() - 1)
        peekTwoChars(c, c2)
    }

    private fun peekTwoChars(c: Char, c2: Char) {
        if (binaryOpAvailable) {
            if (c == '&' && c2 == '&') {
                type = AND
                binaryOpAvailable = false
                return
            } else if (c == '|' && c2 == '|') {
                type = OR
                binaryOpAvailable = false
                return
            } else if (c == '=' && c2 == '=') {
                type = EQ
                binaryOpAvailable = false
                return
            } else if (c == '!' && c2 == '=') {
                type = NE
                binaryOpAvailable = false
                return
            } else if (c == '>' && c2 == '=') {
                type = GE
                binaryOpAvailable = false
                return
            } else if (c == '<' && c2 == '=') {
                type = LE
                binaryOpAvailable = false
                return
            }
        }
        buf.position(buf.position() - 1)
        peekOneChar(c)
    }

    private fun peekOneChar(c: Char) {
        if (binaryOpAvailable) {
            if (c == '>') {
                type = GT
                binaryOpAvailable = false
                return
            } else if (c == '<') {
                type = LT
                binaryOpAvailable = false
                return
            }
        }
        if (Character.isWhitespace(c)) {
            type = WHITESPACE
            return
        } else if (c == ',') {
            type = COMMA
            return
        } else if (c == '(') {
            type = OPEN_BRACKET
            return
        } else if (c == ')') {
            type = CLOSE_BRACKET
            binaryOpAvailable = true
            return
        } else if (c == '!') {
            type = NOT
            return
        } else if (c == '\'') {
            type = CHAR
            if (buf.hasRemaining()) {
                buf.get()
                if (buf.hasRemaining()) {
                    val c3 = buf.get()
                    if (c3 == '\'') {
                        binaryOpAvailable = true
                        return
                    }
                }
            }
            throw ExprException("The end of single quotation mark is not found at $location")
        } else if (c == '"') {
            type = STRING
            var closed = false
            while (buf.hasRemaining()) {
                val c2 = buf.get()
                if (c2 == '"') {
                    if (buf.hasRemaining()) {
                        buf.mark()
                        val c3 = buf.get()
                        if (c3 != '"') {
                            buf.reset()
                            closed = true
                            break
                        }
                    } else {
                        closed = true
                    }
                }
            }
            if (!closed) {
                throw ExprException("The end of double quotation mark is not found at $location")
            }
            binaryOpAvailable = true
        } else if (c == '+' || c == '-') {
            buf.mark()
            if (buf.hasRemaining()) {
                val c2 = buf.get()
                if (Character.isDigit(c2)) {
                    peekNumber()
                    return
                }
                buf.reset()
            }
            type = ILLEGAL_NUMBER
        } else if (Character.isDigit(c)) {
            peekNumber()
        } else if (Character.isJavaIdentifierStart(c)) {
            type = VALUE
            binaryOpAvailable = true
            while (buf.hasRemaining()) {
                buf.mark()
                val c2 = buf.get()
                if (!Character.isJavaIdentifierPart(c2)) {
                    buf.reset()
                    break
                }
            }
        } else if (c == '.') {
            type = PROPERTY
            binaryOpAvailable = true
            if (!buf.hasRemaining()) {
                throw ExprException("Either property or function name must follow the dot at $location")
            }
            buf.mark()
            val c2 = buf.get()
            if (Character.isJavaIdentifierStart(c2)) {
                while (buf.hasRemaining()) {
                    buf.mark()
                    val c3 = buf.get()
                    if (!Character.isJavaIdentifierPart(c3)) {
                        if (c3 == '(') {
                            type = FUNCTION
                            binaryOpAvailable = false
                        }
                        buf.reset()
                        return
                    }
                }
            } else {
                throw ExprException("The character \"$c2\" is illegal as an identifier start at $location")
            }
        } else {
            type = OTHER
        }
    }

    private fun peekNumber() {
        type = INT
        var decimal = false
        while (buf.hasRemaining()) {
            buf.mark()
            val c2 = buf.get()
            if (Character.isDigit(c2)) {
                continue
            } else if (c2 == '.') {
                if (decimal) {
                    type = ILLEGAL_NUMBER
                    return
                }
                decimal = true
                if (buf.hasRemaining()) {
                    val c3 = buf.get()
                    if (!Character.isDigit(c3)) {
                        type = ILLEGAL_NUMBER
                        return
                    }
                } else {
                    type = ILLEGAL_NUMBER
                    return
                }
            } else if (c2 == 'F') {
                type = FLOAT
                break
            } else if (c2 == 'D') {
                type = DOUBLE
                break
            } else if (c2 == 'L') {
                type = LONG
                break
            } else if (c2 == 'B') {
                type = BIG_DECIMAL
                break
            } else {
                buf.reset()
                break
            }
        }
        if (!isWordTerminated()) {
            type = ILLEGAL_NUMBER
        }
        binaryOpAvailable = true
    }

    private fun isWordTerminated(): Boolean {
        buf.mark()
        if (buf.hasRemaining()) {
            val c = buf.get()
            if (!Character.isJavaIdentifierPart(c)) {
                buf.reset()
                return true
            }
        } else {
            return true
        }
        return false
    }

}