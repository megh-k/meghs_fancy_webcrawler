import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

class WebCrawler(
    val seedUrl: String,
) {
    val domain: String = Url(seedUrl).host
    var lookUpUrls: MutableSet<String> = mutableSetOf()

    // todo: implement channel
    var queue: ArrayDeque<String> = ArrayDeque()

    fun crawl(): List<String> {
        queue.add(seedUrl)
        while (queue.isNotEmpty()) {
            val urlStr = queue.removeFirst()
            scrapeUrls(urlStr)
            println("size of queue: ${queue.size}")
            println("size of lookUpUrls: ${lookUpUrls.size}")
        }
        return lookUpUrls.toList()
    }

    fun scrapeUrls(urlStr: String) =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                val response: HttpResponse = client.get(urlStr)
                val doc = Jsoup.parse(response.bodyAsText())
                val relativeUrlsInDoc =
                    doc
                        .select("a[href]")
                        .map { it.attr("href") }
                        .filter { href ->
                            href.startsWith("/")
                        }.map { "http://$domain$it" }
                relativeUrlsInDoc.forEach {
                    addUrl(it)
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
                println(urlStr)
            } finally {
                client.close()
            }
        }

    fun addUrl(urlStr: String) {
        if (!lookUpUrls.contains(urlStr)) {
            queue.add(urlStr)
            lookUpUrls.add(urlStr)
        }
    }
}

fun main() {
    while (true) {
        print("Enter a URL: ")
        val inputUrl = readln()
        if (inputUrl == "exit") {
            println("Exiting program...")
            break
        }
        println("URLs for $inputUrl: ")
        val urls = WebCrawler(inputUrl).crawl()
        for (url in urls) {
            println(url)
        }
    }
}
