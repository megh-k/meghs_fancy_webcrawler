package webcrawler

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class UtilsTest {
    @Test
    fun validateUrlWithProtocol() {
        assertEquals(true, validateUrl("http://www.example.com"))
    }

    @Test
    fun validateUrlWithoutProtocol() {
        assertEquals(true, validateUrl("www.example.com"))
    }

    @Test
    fun validateUrlNotAUrl() {
        assertEquals(false, validateUrl("this is clearly not a url"))
    }

    @Test
    fun normaliseUrlStrWithoutProtocol() {
        assertEquals("http://www.example.com", normaliseUrlStr("www.example.com"))
    }

    @Test
    fun normaliseUrlStrHttp() {
        assertEquals("http://www.example.com", normaliseUrlStr("http://www.example.com"))
    }

    @Test
    fun normaliseUrlStrHttps() {
        assertEquals("https://www.example.com", normaliseUrlStr("https://www.example.com"))
    }
}
