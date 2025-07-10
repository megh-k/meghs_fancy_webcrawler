package webcrawler

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class UtilsTest {
    @Test
    fun `validateUrl with http protocol`() {
        assertEquals(true, validateUrl("http://www.example.com"))
    }

    @Test
    fun `validateUrl without protocol`() {
        assertEquals(true, validateUrl("www.example.com"))
    }

    @Test
    fun `validateUrl invalid url`() {
        assertEquals(false, validateUrl("this is clearly not a url"))
    }

    @Test
    fun `normaliseUrlStr without protocol`() {
        assertEquals("http://www.example.com", normaliseUrlStr("www.example.com"))
    }

    @Test
    fun `normaliseUrlStr with http protocol`() {
        assertEquals("http://www.example.com", normaliseUrlStr("http://www.example.com"))
    }

    @Test
    fun `normaliseUrlStr with https protocol`() {
        assertEquals("https://www.example.com", normaliseUrlStr("https://www.example.com"))
    }
}
