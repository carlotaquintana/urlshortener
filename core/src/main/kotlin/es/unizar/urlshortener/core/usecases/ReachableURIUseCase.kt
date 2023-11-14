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

        // Crea una request
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()

        // Realiza la petición
        val response = client.send(request, HttpResponse.BodyHandlers.discarding())

        // Si la respuesta es 4xx o 5xx, la URI no es alcanzable ya que el servidor no la ha encontrado
        return response.statusCode() < 400
    }
}