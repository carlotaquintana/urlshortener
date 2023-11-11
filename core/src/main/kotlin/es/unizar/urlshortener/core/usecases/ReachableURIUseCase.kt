package es.unizar.urlshortener.core.usecases

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/*
* We want to verify that a URI is reachable.
*/

interface ReachableURIUseCase {
    fun reachable(uri: String): Boolean
}

class ReachableURIUseCaseImpl : ReachableURIUseCase {
    override fun reachable(uri: String): Boolean {
        // To verify if a URI is reachable we need to send a request to the URI and check the response.
        // We create a new HTTP client.
        val client = HttpClient.newHttpClient()

        // We create a new HTTP request.
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()

        // We send the request and check the response.
        val response = client.send(request, HttpResponse.BodyHandlers.discarding())

        // If the response is 200, the URI is reachable. If not, it isn't reachable.
        return response.statusCode() == 200

    }
}