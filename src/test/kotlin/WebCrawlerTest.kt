import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WebCrawlerTest {
    private lateinit var testWebCrawler: WebCrawler
    private lateinit var testClient: HttpClient

    @BeforeEach
    fun setUp() {
        val mockEngine =
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/" ->
                        respond(
                            content = loadFixture("home"),
                            headers = headersOf("Content-Type", "text/html"),
                            status = HttpStatusCode.OK,
                        )
                    "/blog" ->
                        respond(
                            content = loadFixture("blog"),
                            headers = headersOf("Content-Type", "text/html"),
                            status = HttpStatusCode.OK,
                        )
                    "/careers" ->
                        respond(
                            content = loadFixture("careers"),
                            headers = headersOf("Content-Type", "text/html"),
                            status = HttpStatusCode.OK,
                        )
                    "/contact" ->
                        respond(
                            content = loadFixture("contact"),
                            headers = headersOf("Content-Type", "text/html"),
                            status = HttpStatusCode.OK,
                        )
                    else -> respondError(HttpStatusCode.NotFound)
                }
            }
        testClient = HttpClient(mockEngine)
        testWebCrawler = WebCrawler("www.example.com/", testClient)
    }

    fun loadFixture(pageName: String): String = this::class.java.getResource("/fixtures/$pageName.html")!!.readText()

    @Test
    fun `web crawler initiates as expected`() {
        assertEquals("http://www.example.com/", testWebCrawler.seedUrl)
        assertEquals("www.example.com", testWebCrawler.domain)
        assertEquals(mutableSetOf<String>(), testWebCrawler.urlSet)
        assertEquals(1, testWebCrawler.queueSize.get())
        assertEquals(0, testWebCrawler.activeWorkers.get())
    }

    @Test
    fun `crawl finds all urls linked`() =
        runBlocking {
            val urls = testWebCrawler.crawl()
            assertEquals(
                listOf<String>(
                    "http://www.example.com/careers",
                    "http://www.example.com/blog",
                    "http://www.example.com/contact",
                    "http://www.example.com/",
                ),
                urls,
            )
            assertEquals(0, testWebCrawler.queueSize.get())
            assertEquals(0, testWebCrawler.activeWorkers.get())
        }

    @Test
    fun `scrapeUrls finds all urls on given url`() =
        runBlocking {
            testWebCrawler.scrapeUrls("http://www.example.com/careers")
            assertEquals(mutableSetOf<String>("http://www.example.com/contact"), testWebCrawler.urlSet)
        }

    @Test
    fun `addUrl only adds unseen urls to urlSet`() =
        runBlocking {
            testWebCrawler.addUrl("http://www.example.com")
            assertEquals(mutableSetOf<String>("http://www.example.com"), testWebCrawler.urlSet)
            assertEquals(2, testWebCrawler.queueSize.get())
            testWebCrawler.addUrl("http://www.example.com/careers")
            assertEquals(mutableSetOf<String>("http://www.example.com", "http://www.example.com/careers"), testWebCrawler.urlSet)
            assertEquals(3, testWebCrawler.queueSize.get())
            testWebCrawler.addUrl("http://www.example.com/careers")
            assertEquals(3, testWebCrawler.queueSize.get())
            assertEquals(mutableSetOf<String>("http://www.example.com", "http://www.example.com/careers"), testWebCrawler.urlSet)
        }

    @Test
    fun `getRelativeUrls only gets relative urls and not mailto`() =
        runTest {
            val response = testClient.get("http://www.example.com/contact")

            testWebCrawler.getRelativeUrls(response)
            assertEquals(mutableSetOf<String>("http://www.example.com/careers"), testWebCrawler.urlSet)
        }
}
