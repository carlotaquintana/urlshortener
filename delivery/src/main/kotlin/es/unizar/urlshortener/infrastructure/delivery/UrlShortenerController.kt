package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val reachableURIUseCase: ReachableURIUseCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        redirectUseCase.redirectTo(id).let { targetUri ->
            reachableURIUseCase.reachable(targetUri.target).let { reachable ->
                if (reachable) {
                    logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                    val h = HttpHeaders()
                    h.location = URI.create(targetUri.target)
                    return ResponseEntity<Unit>(h, HttpStatus.valueOf(targetUri.mode))
                }
                else{
                    // No es alcanzable. Devuelve respuesta con estado 403
                    val h = HttpHeaders()
                    return ResponseEntity<Unit>(h, HttpStatus.FORBIDDEN)
                }
            }
        }
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        // Hace 3 peticiones GET a la URI de destino para comprobar si es alcanzable
        val reachable = isUriReachable(data.url)

        if (!reachable) {
            val errorResponse = ShortUrlDataOut(
                url = null,
                properties = mapOf("error" to "The destination URI is not reachable.")
            )
            val h = HttpHeaders()
            return ResponseEntity<ShortUrlDataOut>(errorResponse, h, HttpStatus.BAD_REQUEST)
        }
        return createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url

            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )

            // Si la solicitud es procesada con éxito devuelve una respuesta con estado 201,
            // una cabecera con la ruta a la URI recortada e información JSON sobre la URI recortada
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    }
    // 3 peticiones GET para comprobar si la URI de destino es alcanzable
    private fun isUriReachable(uri: String): Boolean {
        var attempt = 0
        while (attempt < 3) {
            try {
                val connection = URL(uri).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true
                }
            } catch (e: Exception) {
                // Cualquier excepción se considera que la URI no es alcanzable
                return false
            }

            attempt++
            // Espaciar las peticiones un segundo
            TimeUnit.SECONDS.sleep(1)
        }

        return false
    }
}
