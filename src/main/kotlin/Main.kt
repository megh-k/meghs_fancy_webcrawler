package webcrawler
import kotlinx.coroutines.runBlocking

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
