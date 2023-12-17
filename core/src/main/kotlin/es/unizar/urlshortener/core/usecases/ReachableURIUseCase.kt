package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.queues.ColaAlcanzable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.OffsetDateTime
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * Constantes.
 */
private const val MAX_ATTEMPTS: Int = 3
private const val ATTEMPT_DELAY_SECONDS: Long = 1L
private const val CLIENT_TIMEOUT: Long = 10L

/**
* Class that verifies if a URI is reachable.
*
* *[reachable]* checks if a URI is reachable or not.
*
*/
interface ReachableURIUseCase {
    /**
     * Comprueba si una URI es alcanzable o no.
     * @param uri URI a comprobar
     * @return true si es alcanzable, false si no lo es
     */
    fun reachable(uri: String): Boolean

    /**
     * Verifica si una URI es alcanzable o no.
     * @param uri URI a comprobar
     */
    fun verifyReachability(uri:String)
}

/**
 * Implementation of [ReachableURIUseCase].
 */
@Suppress("SwallowedException", "NewLineAtEndOfFile", "MagicNumber")
class ReachableURIUseCaseImpl (
    private val uriMap : HashMap<String, Pair<Boolean, OffsetDateTime>>
): ReachableURIUseCase {

    private val logger: Logger = LogManager.getLogger(ReachableURIUseCase::class.java)
    /**
     * Comprueba si una URI es alcanzable o no.
     * @param uri URI a comprobar
     * @return true si es alcanzable, false si no lo es
     */
    override fun reachable(uri: String): Boolean {
        // Busca la URI en el mapa y mira si es alcanzable o no
        return uriMap[uri]?.first ?: false
    }

    /**
     * Verifica si una URI es alcanzable o no.
     * @param uri URI a comprobar
     */
    override fun verifyReachability(uri:String) {
        // Se coge la URI y se comprueba si es alcanzable haciendo una petición GET a la misma
        // y si devuelve 200, se considera como alcanzable.
        // Crea un cliente con un timeout de 10s
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CLIENT_TIMEOUT)).build()

        for (i in 1..MAX_ATTEMPTS) {
            println("Intento $i de $MAX_ATTEMPTS")
            // Crea la petición
            val request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()

            // Realiza la petición de forma asíncrona
            val responseFuture: CompletableFuture<HttpResponse<Void>> =
                client.sendAsync(request, HttpResponse.BodyHandlers.discarding())

            try {
                // Espera a que la petición termine
                val response = responseFuture.get()

                // Si la respuesta es 200, se considera como alcanzable
                if (response.statusCode() == 200) {
                    uriMap[uri] = Pair(true, OffsetDateTime.now())
                    logger.info("La URI $uri es alcanzable")
                    break
                } else {
                    uriMap[uri] = Pair(false, OffsetDateTime.now())
                    logger.info("La URI $uri no es alcanzable")
                }
            } catch (e: ExecutionException) {
                // La petición lanzó una excepción, se considera como no alcanzable
                uriMap[uri] = Pair(false, OffsetDateTime.now())

            } catch (e: java.util.concurrent.TimeoutException) {
                // La petición excedió el tiempo de espera, se considera como no alcanzable
                uriMap[uri] = Pair(false, OffsetDateTime.now())
            }

            // Pausa de 1 segundo entre intentos
            Thread.sleep(Duration.ofSeconds(ATTEMPT_DELAY_SECONDS).toMillis())

        }
    }
}
