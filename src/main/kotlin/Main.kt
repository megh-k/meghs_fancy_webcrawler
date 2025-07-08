import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.jsoup.Jsoup
import java.net.URI
import java.net.URISyntaxException
import java.util.Collections.synchronizedSet
import java.util.concurrent.atomic.AtomicInteger

class WebCrawler(
    seedUrl: String,
) {
    val seedUrl: String =
        normaliseUrlStr(seedUrl).also {
            require(validateUrl(it)) { "Whoops, \"$it\" is not a valid URL, please try something like http://www.example.com" }
        }
    val domain: String = Url(this.seedUrl).host
    var urlSet: MutableSet<String> = synchronizedSet(mutableSetOf<String>())
    val queue = Channel<String>(100)
    val queueSize = AtomicInteger(1)
    var activeWorkers = AtomicInteger(0)

    suspend fun crawl(): List<String> {
        queue.send(seedUrl)
        supervisorScope {
            while (isActive) {
                val urlStr = queue.receiveCatching().getOrNull() ?: break

                launch {
                    try {
                        scrapeUrls(urlStr)
                    } finally {
                        activeWorkers.decrementAndGet()
                        queueSize.decrementAndGet()
                        if (queueSize.get() == 0 && activeWorkers.get() == 0) {
                            queue.close()
                        }
                    }
                }
            }
        }
        return urlSet.toList()
    }

    suspend fun scrapeUrls(urlStr: String) {
        activeWorkers.incrementAndGet()
        val client = HttpClient(CIO)
        try {
            val response: HttpResponse = client.get(urlStr)
            getRelativeUrls(response)
        } catch (e: Exception) {
            println("Request to $urlStr failed: ${e.message}")
        } finally {
            client.close()
        }
    }

    fun validateUrl(urlStr: String): Boolean {
        try {
            // note: io.ktor.http.Url does not validate
            URI(urlStr)
            return true
        } catch (_: URISyntaxException) {
            return false
        }
    }

    fun normaliseUrlStr(urlStr: String): String =
        if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
            urlStr
        } else {
            "http://$urlStr"
        }

    suspend fun addUrl(urlStr: String) {
        if (!urlSet.contains(urlStr)) {
            queue.send(urlStr)
            queueSize.incrementAndGet()
            urlSet.add(urlStr)
        }
    }

    suspend fun getRelativeUrls(response: HttpResponse) {
        val doc = Jsoup.parse(response.bodyAsText())
        doc
            .select("a[href]")
            .map { it.attr("href") }
            .filter {
                it.startsWith("/")
            }.map { "http://$domain$it" }
            .forEach {
                addUrl(it)
            }
    }
}

fun main() =
    runBlocking {
        while (true) {
            print("Enter a URL: ")
            val inputUrl = readln()
            if (inputUrl == "exit") {
                println("Exiting program...")
                break
            }
            println("URLs for $inputUrl: ")

            val urls = WebCrawler(inputUrl).crawl()
            urls.forEach { println(it) }
        }
    }
