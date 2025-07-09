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

/**
 * Web crawler
 *
 * Given a starting url, scrape the domain for all relative URLs.
 *
 * @constructor Represents the given [seedUrl] (normalised), the domain,
 * a set of url strings, a queue, queue size and the current active workers
 *
 * @param seedUrl the starting point of the webcrawl
 * @property seedUrl the starting point of the webcrawl
 * @property domain the domain of the [seedUrl] provided
 * @property urlSet a set of relative URLs scraped from the domain
 * @property queue URLs that still need to be visited to find more URLs
 * @property queueSize Size of the queue, starts at 1 as [seedUrl] gets added to the queue on crawl
 * @property activeWorkers number of workers currently in use, starts at 0
 */
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

    /**
     * Crawl
     *
     * gets a url string off the queue.
     * tries to scrape the page
     * if successful, decreases the number of workers and queue size
     * if both of those variables are 0, close the queue and exit the app
     *
     * @return a list of the Urls found on a webpage
     */
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

    /**
     * Scrape urls
     *
     * function executed by the workers.
     * number of active workers incremented
     * Http request made to [urlStr]
     *
     * @param urlStr
     */
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

    /**
     * Validate url
     *
     * Construct a URI from [urlStr]
     * if this succeeds, validation passes
     * if this fails, validation fails.
     *
     * @param urlStr
     * @return pass/fail Boolean
     */
    fun validateUrl(urlStr: String): Boolean {
        try {
            // note: io.ktor.http.Url does not validate
            URI(urlStr)
            return true
        } catch (_: URISyntaxException) {
            return false
        }
    }

    /**
     * Normalise url str
     *
     *  Normalise the [urlStr] such that url's without a protocol are supported
     *
     * @param urlStr
     * @return a normalised [urlStr] with a protocol present
     */
    fun normaliseUrlStr(urlStr: String): String =
        if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
            urlStr
        } else {
            "http://$urlStr"
        }

    /**
     * Add url
     *
     * If a url has not been found previously,
     * add the [urlStr] to the queue
     * increment to queue size
     * add the [urlStr] to the set of found urls
     *
     * @param urlStr
     */
    suspend fun addUrl(urlStr: String) {
        if (!urlSet.contains(urlStr)) {
            queue.send(urlStr)
            queueSize.incrementAndGet()
            urlSet.add(urlStr)
        }
    }

    /**
     * Get relative urls
     *
     * parse the [response] body and find all relative urls within the html.
     * map the relative url to an absolute
     * pass each url to addUrl
     *
     * @param response HttpResponse
     */
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

/**
 * Main
 *
 */
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
