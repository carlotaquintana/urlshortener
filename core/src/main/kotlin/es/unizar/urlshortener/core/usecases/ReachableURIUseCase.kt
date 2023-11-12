package es.unizar.urlshortener.core.usecases

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/*
* We want to verify that a URI is reachable.
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
    private val ATTEMPT_DELAY_SECONDS = 1L
    override fun reachable(uri: String): Boolean {
        var attempts = 0
        while (attempts < MAX_ATTEMPTS) {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(ATTEMPT_DELAY_SECONDS))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()

            val response = try {
                client.send(request, HttpResponse.BodyHandlers.discarding())
            } catch (e: Exception) {
                // Manejar cualquier excepción que pueda ocurrir durante la solicitud
                null
            }

            if (response != null && response.statusCode() == 200) {
                return true
            }

            attempts++
            Thread.sleep(ATTEMPT_DELAY_SECONDS * 1000) // Espaciar las solicitudes en 1 segundo
        }
        return false

    }
}