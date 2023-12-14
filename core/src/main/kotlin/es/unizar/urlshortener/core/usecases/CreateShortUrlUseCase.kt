@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    // Create a short url from a long url
    override fun create(url: String, data: ShortUrlProperties): ShortUrl {
        // Check if the URL is valid
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        }


        // Generate hash and create short URL
        val id: String = hashService.hasUrl(url)
        val su = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = ShortUrlProperties(
                safe = data.safe,
                ip = data.ip,
                sponsor = data.sponsor
            )
        )

        // Save short URL
        shortUrlRepository.save(su)

        return su
    }
}
