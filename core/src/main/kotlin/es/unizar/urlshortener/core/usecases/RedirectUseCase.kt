package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.time.OffsetDateTime

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
    fun isLimitExceeded(key: String): Boolean
    fun getLimit(id: String): Int
    fun getRedirectionCount(key: String): Int
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String): Redirection {
        val shortUrl = shortUrlRepository.findByKey(key)
            ?: throw RedirectionNotFound(key)

        // Incrementar el contador de redirecciones
        val updatedRedirectCount = shortUrl.properties.redirectCount + 1

        // Crear una nueva instancia de ShortUrlProperties con el contador actualizado
        // y la hora actual
        val updatedProperties = shortUrl.properties.copy(
            redirectCount = updatedRedirectCount,
            lastRedirectDateTime = OffsetDateTime.now()
        )

        // Crear una nueva instancia de ShortUrl con las propiedades actualizadas
        val updatedShortUrl = shortUrl.copy(properties = updatedProperties)

        // Guardar la URL corta actualizada en el repositorio
        shortUrlRepository.save(updatedShortUrl)
        return updatedShortUrl.redirection
    }
    override fun isLimitExceeded(key: String): Boolean {
        val shortUrl = shortUrlRepository.findByKey(key)

        // Se verifica el límite en la última hora
        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val redirectionsInLastHour = shortUrl?.properties?.lastRedirectDateTime?.isAfter(oneHourAgo)

        return shortUrl?.properties?.limit != null && shortUrl.properties.limit < shortUrl.properties.redirectCount
    }

    override fun getLimit(id: String): Int {
        return shortUrlRepository.findByKey(id)?.properties?.limit ?: 0
    }

    override fun getRedirectionCount(key: String): Int {
        return shortUrlRepository.findByKey(key)?.properties?.redirectCount ?: 0
    }

}

