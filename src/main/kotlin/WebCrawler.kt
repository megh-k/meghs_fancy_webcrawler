import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.jsoup.Jsoup
import java.util.Collections
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
 * @param client Http client with a default
 * @property seedUrl the starting point of the webcrawl
 * @property domain the domain of the [seedUrl] provided
 * @property urlSet a set of relative URLs scraped from the domain
 * @property queue URLs that still need to be visited to find more URLs
 * @property queueSize Size of the queue, starts at 1 as [seedUrl] gets added to the queue on crawl
 * @property activeWorkers number of workers currently in use, starts at 0
 */
class WebCrawler(
    seedUrl: String,
    private val client: HttpClient = HttpClient(CIO),
) {
    val seedUrl: String =
        normaliseUrlStr(seedUrl).also {
            require(validateUrl(it)) { "Whoops, \"$it\" is not a valid URL, please try something like http://www.example.com" }
        }
    val domain: String = Url(this.seedUrl).host
    var urlSet: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
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
        try {
            val response: HttpResponse = client.get(urlStr)
            getRelativeUrls(response)
        } catch (e: Exception) {
            println("Request to $urlStr failed: ${e.message}")
        }
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
