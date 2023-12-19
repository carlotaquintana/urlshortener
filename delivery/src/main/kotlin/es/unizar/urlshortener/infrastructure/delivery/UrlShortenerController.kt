package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.queues.QRCola
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LimitUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QrUseCase
import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import java.util.concurrent.BlockingQueue
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/** The specification of the controller. */
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
    fun shortener(
            data: ShortUrlDataIn,
            request: HttpServletRequest,
            limit: Int? = null
    ): ResponseEntity<ShortUrlDataOut>

    /** Generates a QR code for a short url identified by its [id]. */
    fun generateQR(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>
}

/** Data required to create a short url. */
data class ShortUrlDataIn(
        val url: String,
        val sponsor: String? = null,
        val qr: Boolean,
        val limit: Int? = null
)
/** Service responsible for processing messages in the queue to update the redirection counter. */
@Service
@Suppress("SwallowedException", "ReturnCount", "UnusedPrivateProperty", "WildcardImport")
class RedirectCounterService(
        private val meterRegistry: MeterRegistry,
        private val clickRepositoryService: ClickRepositoryService,
        @Qualifier("reachableQueueMetric") private val reachableQueueMetric: BlockingQueue<String>
) {

    // scheduled task at start
    fun compute() {
        while (true) {
            if (!reachableQueueMetric.isEmpty()) {
                val uri = reachableQueueMetric.take()

                if (uri == "app.metric.redirect_counter") {
                    // Actualiza el contador usando el registro de métricas
                    meterRegistry.gauge("app.metric.redirect_counter", clickRepositoryService) {
                        it.counter().toDouble()
                    }
                }
            }
        }
    }

    // Método para limpiar la cola al inicio
    fun clearQueue() {
        reachableQueueMetric.clear()
    }

    // Método para agregar mensajes a la cola reachableQueueMetric
    fun addToQueue(message: String) {
        reachableQueueMetric.put(message)
    }
}

/** Data returned after the creation of a short url. */
data class ShortUrlDataOut(val url: URI? = null, val properties: Map<String, Any?> = emptyMap())

@Service
@Suppress("SwallowedException", "ReturnCount", "UnusedPrivateProperty", "WildcardImport")
class UriCounterService(
        private val meterRegistry: MeterRegistry,
        private val shortUrlRepositoryService: ShortUrlRepositoryService,
        @Qualifier("uriQueueMetric") private val uriQueueMetric: BlockingQueue<String>
) {

    fun compute() {
        while (true) {
            if (!uriQueueMetric.isEmpty()) {
                val uri = uriQueueMetric.take()

                if (uri == "app.metric.uri_counter") {
                    // Actualiza el contador usando el registro de métricas
                    meterRegistry.gauge("app.metric.uri_counter", shortUrlRepositoryService) {
                        it.counter().toDouble()
                    }
                }
            }
        }
    }

    // Método para limpiar la cola al inicio
    fun clearQueue() {
        uriQueueMetric.clear()
    }

    // Método para agregar mensajes a la cola reachableQueueMetric
    fun addToQueue(message: String) {
        uriQueueMetric.put(message)
    }
}

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
@Suppress(
        "SwallowedException",
        "ReturnCount",
        "UnusedPrivateProperty",
        "WildcardImport",
        "LongParameterList"
)
class UrlShortenerControllerImpl(
        val redirectUseCase: RedirectUseCase,
        val logClickUseCase: LogClickUseCase,
        val createShortUrlUseCase: CreateShortUrlUseCase,
        val limitUseCase: LimitUseCase,
        val qrUseCase: QrUseCase,
        val qrQueue: BlockingQueue<Pair<String, String>>,
        val reachableURIUseCase: ReachableURIUseCase,
        val reachableQueue: BlockingQueue<String>
) : UrlShortenerController {

    private val logger: Logger = LogManager.getLogger(UrlShortenerController::class.java)

    /* Atrapa todo lo que no empieza por lo especificado */
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(
            @PathVariable id: String,
            request: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {
        try {
            // Cada vez que se llama a redirectTo se incrementa el número de redirecciones.
            // Si se ha excedido el límite, se devuelve un 429 Too Many Requests
            if (limitUseCase.limitExceeded(id)) {
                logger.info("Se ha excedido el límite de redirecciones para $id")
                val h = HttpHeaders()
                return ResponseEntity(h, HttpStatus.TOO_MANY_REQUESTS)
            }
            val redirectionResult = redirectUseCase.redirectTo(id)
            // Mirar si es alcanzable
            if (reachableURIUseCase.reachable(redirectionResult.target)) {
                // Se ha comprobado que es alcanzable, se redirige y se loguea
                logger.info("La uri ${redirectionResult.target} es alcanzable, redirigiendo...")
                logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                val h = HttpHeaders()
                h.location = URI.create(redirectionResult.target)
                return ResponseEntity(h, HttpStatus.valueOf(redirectionResult.mode))
            } else {
                // El id está registrado pero aún no se ha confirmado que sea alcanzable. Se
                // devuelve una respuesta con estado 400 Bad Request y una cabecera
                // Retry-After indicando cuanto tiempo se debe esperar antes de volver a intentarlo
                logger.info(
                        "La uri ${redirectionResult.target} no es alcanzable, devolviendo error 400"
                )
                val h = HttpHeaders()
                h.set("Retry-After", "10")
                return ResponseEntity(h, HttpStatus.BAD_REQUEST)
            }
        } catch (e: RedirectionNotFound) {
            // Si el id no está registrado se devuelve un error 404
            logger.info("La uri $id no está registrada, devolviendo error 404")
            val h = HttpHeaders()
            return ResponseEntity(h, HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(
            data: ShortUrlDataIn,
            request: HttpServletRequest,
            limit: Int?
    ): ResponseEntity<ShortUrlDataOut> =
            createShortUrlUseCase.create(
                            url = data.url,
                            data =
                                    ShortUrlProperties(
                                            ip = request.remoteAddr,
                                            sponsor = data.sponsor,
                                            qr = data.qr,
                                            limit = limit
                                    )
                    )
                    .let {
                        val h = HttpHeaders()
                        val url =
                                linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }
                                        .toUri()
                        h.location = url

                        // En el caso que el qr sea true, se añaden el id del qr y la url a la cola
                        if (data.qr) {
                            qrQueue.add(Pair(it.hash, url.toString()))
                        }

                        // Se mira el límite y si es negativo se devuelve un 400
                        if (limit != null && limit < 0) {
                            logger.error("El límite de redirecciones no puede ser negativo")
                            return ResponseEntity.badRequest().build()
                        } else {
                            // Se añade la nueva redirección
                            logger.info(
                                    "Se ha añadido la redirección ${data.url} con límite $limit"
                            )
                            limitUseCase.newRedirect(data.url, limit ?: 0)
                        }

                        // Se añade la URI a la cola para verificación asíncrona mirando primero
                        // si hay capacidad en la cola
                        try {
                            if (!reachableQueue.offer(data.url)) {
                                logger.error(
                                        "No se ha podido añadir la URI ${data.url} a la cola. Está llena."
                                )
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .build()
                            } else {
                                logger.info("Se ha añadido la URI ${data.url} a la cola.")
                                // Se añade la URI a la cola para verificación asíncrona
                                reachableQueue.put(data.url)
                            }
                        } catch (e: InterruptedException) {
                            logger.error(
                                    "No se ha podido añadir la URI ${data.url} a la cola. Ha ocurrido un error."
                            )
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                        }
                        val response =
                                ShortUrlDataOut(
                                        url = url,
                                        properties =
                                                mapOf(
                                                        "safe" to it.properties.safe,
                                                        "limit" to it.properties.limit
                                                ) +
                                                        when (data.qr) {
                                                            true ->
                                                                    mapOf(
                                                                            "qr" to
                                                                                    linkTo<
                                                                                                    UrlShortenerControllerImpl> {
                                                                                                generateQR(
                                                                                                        it.hash,
                                                                                                        request
                                                                                                )
                                                                                            }
                                                                                            .toUri()
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
            qrUseCase.getQR(id).let {
                val logger: Logger = LogManager.getLogger(QRCola::class.java)
                logger.info("QR $id")
                val headers = HttpHeaders()
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                ResponseEntity<ByteArrayResource>(
                        ByteArrayResource(it, MediaType.IMAGE_PNG_VALUE),
                        headers,
                        HttpStatus.OK
                )
            }

    @GetMapping("/api/link/{id}")
    fun getLinkInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val limitAndRedirections = limitUseCase.getLimitAndRedirections(id)

        return if (limitAndRedirections != null) {
            val responseMap =
                    mapOf(
                            "limit" to limitAndRedirections.first,
                            "redirections" to limitAndRedirections.second
                    )
            ResponseEntity(responseMap, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
