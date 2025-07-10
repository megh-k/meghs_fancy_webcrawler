import java.net.URI
import java.net.URISyntaxException

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
