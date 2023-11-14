package es.unizar.urlshortener.core.usecases

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
* Class that verifies if a URI is reachable.
*
* *[reachable]* checks if a URI is reachable or not.
*
*/

interface ReachableURIUseCase {
    fun reachable(uri: String): Boolean
}

/**
 *
 */
class ReachableURIUseCaseImpl : ReachableURIUseCase {
    private val MAX_ATTEMPTS = 3
    private val ATTEMPT_DELAY_SECONDS = 1L // 1s de delay
    override fun reachable(uri: String): Boolean {
        // Crea un cliente
        val client = HttpClient.newBuilder().build()

        // Máximo de 3 peticiones GET espaciadas 1s
        for (attempt in 1..MAX_ATTEMPTS){
            val request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()

            // Realiza la petición
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())

            // Si la respuesta es 200, se retorna (correcto)
            if (response.statusCode() == 200) {
                return true
            }

            // Se espera 1s a hacer la siguiente petición
            Thread.sleep(Duration.ofSeconds(ATTEMPT_DELAY_SECONDS).toMillis())
        }

        // Si no se ha devuelto ya true es que no es alcanzable
        return false
    }
}