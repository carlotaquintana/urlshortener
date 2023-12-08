package es.unizar.urlshortener.core.usecases

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

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
}

/**
 * Implementation of [ReachableURIUseCase].
 */
class ReachableURIUseCaseImpl (): ReachableURIUseCase {
    private val MAX_ATTEMPTS = 3
    private val ATTEMPT_DELAY_SECONDS = 1L // 1s de delay
    override fun reachable(uri: String): Boolean {
        // Crea un cliente con un timeout de 10s
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

        // Máximo de 3 peticiones GET espaciadas 1s
        for (attempt in 1..MAX_ATTEMPTS){
            val request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()

            // Realiza la petición de forma asíncrona
            val responseFuture: CompletableFuture<HttpResponse<Void>> = client.sendAsync(request, HttpResponse.BodyHandlers.discarding())

            try{
                // Espera a que la petición termine
                val response = responseFuture.get()

                // Si la respuesta es 200, se retorna (correcto)
                if (response.statusCode() == 200) {
                    return true
                }
            } catch (e: ExecutionException) {
                // La petición lanzó una excepción, se considera como no alcanzable
                return false
            } catch (e: java.util.concurrent.TimeoutException) {
                // La petición excedió el tiempo de espera, se considera como no alcanzable
                return false
            }

            // Se espera 1s a hacer la siguiente petición
            Thread.sleep(Duration.ofSeconds(ATTEMPT_DELAY_SECONDS).toMillis())
        }

        // Si no se ha devuelto ya true es que no es alcanzable
        return false
    }
}