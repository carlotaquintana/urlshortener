package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
import java.util.concurrent.BlockingQueue



/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Map<String, String>>

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
    meterRegistry: MeterRegistry,
    val reachableURIUseCase: ReachableURIUseCase,
    val reachableQueue: BlockingQueue<String>
) : UrlShortenerController {

    val redirectCounter: Counter = meterRegistry.counter("app.metric.redirect_counter")
    val uriCounter: Counter = meterRegistry.counter("app.metric.uri_counter")

    /* Atrapa todo lo que no empieza por lo especificado */
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        try{
            val redirectionResult = redirectUseCase.redirectTo(id)
            // Mirar si es alcanzable
            if(reachableURIUseCase.reachable(redirectionResult.target)){
                // Se ha comprobado que es alcanzable, se redirige y se loguea
                println("La uri ${redirectionResult.target} es alcanzable, redirigiendo...")
                logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                val h = HttpHeaders()
                h.location = URI.create(redirectionResult.target)
                return ResponseEntity(h, HttpStatus.valueOf(redirectionResult.mode))
            }
            else{
                // El id está registrado pero aún no se ha confirmado que sea alcanzable. Se
                // devuelve una respuesta con estado 400 Bad Request y una cabecera
                // Retry-After indicando cuanto tiempo se debe esperar antes de volver a intentarlo
                println("La uri ${redirectionResult.target} no es alcanzable, devolviendo error 400")
                val h = HttpHeaders()
                h.set("Retry-After", "10")
                return ResponseEntity(h, HttpStatus.BAD_REQUEST)
            }
        }
        catch(e: RedirectionNotFound){
            // Si el id no está registrado se devuelve un error 404
            println("El id $id no está registrado, devolviendo error 404")
            val errorResponse = mapOf("error" to "Redirection not found")
            val h = HttpHeaders()
            return ResponseEntity(h, HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        // Si la URI es alcanzable, se crea la URL corta
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            uriCounter.increment()
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url

            // Se añade la URI a la cola para verificación asíncrona
            reachableQueue.put(data.url)

            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )
            // Se devuelve la respuesta con estado 201 Created
            return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    }
}
