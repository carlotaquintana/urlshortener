package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LimitUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
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
import java.net.URI

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
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest, limit: Int? = null): ResponseEntity<ShortUrlDataOut>

    /**
     * Obtiene información detallada de un short url identificado por su [id].
     */
    fun getShortUrlInfo(id: String): ResponseEntity<ShortUrlDataOut>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val limit: Int? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any?> = emptyMap()
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
    val limitUseCase: LimitUseCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        // Cada vez que se llama a redirectTo se incrementa el número de redirecciones.
        // Si se ha excedido el límite, se devuelve un 429 Too Many Requests
        if(limitUseCase.limitExceeded(id)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()

        return redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }

    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest, limit: Int?): ResponseEntity<ShortUrlDataOut> {
        // Se mira el límite y si es negativo se devuelve un 400
        if (limit != null && limit < 0) {
            return ResponseEntity.badRequest().build()
        }

        val properties = ShortUrlProperties(
            ip = request.remoteAddr,
            sponsor = data.sponsor,
            limit = limit
        )

        // Se añade la nueva redirección
        limitUseCase.newRedirect(data.url, limit ?: 0)

        // Se crea la URL
        val result = createShortUrlUseCase.create(url = data.url, data = properties)

        val h = HttpHeaders()
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(result.hash, request) }.toUri()
        h.location = url

        val response = ShortUrlDataOut(
            url = url,
            properties = mapOf(
                "safe" to result.properties.safe,
                "limit" to properties.limit
            )
        )

        return ResponseEntity(response, h, HttpStatus.CREATED)
    }

    @GetMapping("/api/link/{id}")
    override fun getShortUrlInfo(@PathVariable id: String): ResponseEntity<ShortUrlDataOut> {
        // Obtiene el límite de redirecciones de la URI con identificador id
        val limit = redirectUseCase.getLimit(id)

        val info = ShortUrlDataOut(
            properties = mapOf(
                "limit" to limit,

            )
        )

        return ResponseEntity(info, HttpStatus.OK)
    }

}
