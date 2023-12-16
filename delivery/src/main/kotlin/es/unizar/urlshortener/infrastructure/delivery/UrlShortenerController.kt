package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QrUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ByteArrayResource
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
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Generates a QR code for a short url identified by its [id].
     */
    fun generateQR(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean
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
    val qrUseCase: QrUseCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> =
            redirectUseCase.redirectTo(id).let {
                logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                val h = HttpHeaders()
                h.location = URI.create(it.target)
                ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
            }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
            createShortUrlUseCase.create(
                    url = data.url,
                    data = ShortUrlProperties(
                            ip = request.remoteAddr,
                            sponsor = data.sponsor,
                            qr = data.qr
                    )
            ).let {
                val h = HttpHeaders()
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
                h.location = url
                val response = ShortUrlDataOut(
                        url = url,
                        /*properties = mapOf(
                                "safe" to it.properties.safe,
                                //Se pasa dentro de properties porque shortUrlDataOut esta
                                // definido solo con url y properties
                                "hash" to it.hash

                                //cuando hay qr tiene que devolver una url donde esta el qr
                        )*/
                        // Si data.qr es true, se añade la propiedad qr a la respuesta
                        // Si data.qr es false, crea un mapa vacio
                        properties = when (data.qr) {
                            true -> mapOf(
                                    "qr" to linkTo<UrlShortenerControllerImpl> { generateQR(it.hash, request) }.toUri()
                            )
                            false -> mapOf()
                        }
                )
                ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
            }

    @GetMapping("/{id:(?!api|index).*}/qr")
    override fun generateQR(
            @PathVariable id: String,
            request: HttpServletRequest
    ): ResponseEntity<ByteArrayResource> =

            qrUseCase.getQR(id, linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toString()).let {
                println("QR: " + it)
                val headers = HttpHeaders()
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                ResponseEntity<ByteArrayResource>(ByteArrayResource(it, MediaType.IMAGE_PNG_VALUE), headers, HttpStatus.OK)
            }
}
