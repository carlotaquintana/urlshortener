package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCaseImpl
import es.unizar.urlshortener.core.usecases.LimitUseCaseImpl
import es.unizar.urlshortener.core.usecases.LogClickUseCaseImpl
import es.unizar.urlshortener.core.usecases.QrUseCaseImpl
import es.unizar.urlshortener.core.usecases.ReachableURIUseCaseImpl
import es.unizar.urlshortener.core.usecases.RedirectUseCaseImpl
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.QrServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.time.OffsetDateTime

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
@Suppress("TooManyFunctions", "MagicNumber")
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun limitUseCase() = LimitUseCaseImpl()

    @Bean
    fun qrUseCase() =
        QrUseCaseImpl(shortUrlRepositoryService(), qrMap(), qrService())
    @Bean
    fun qrMap(): HashMap<String, ByteArray> = HashMap()

    @Bean
    fun qrQueue(): BlockingQueue<Pair<String, String>> = LinkedBlockingQueue()

    @Bean
    fun qrService() = QrServiceImpl()


    @Bean
    fun reachableMap(): HashMap<String, Pair<Boolean, OffsetDateTime>> = HashMap()

    @Bean
    fun reachableUriUseCase() = ReachableURIUseCaseImpl(reachableMap())

    @Bean
    @Qualifier("reachableQueue")
    fun reachableQueue() : BlockingQueue<String> = LinkedBlockingQueue(100)

    @Bean
    @Qualifier("reachableQueueMetric")
    fun reachableQueueMetric() : BlockingQueue<String> = LinkedBlockingQueue(100)

    @Bean
    @Qualifier("uriQueueMetric")
    fun uriQueueMetric() : BlockingQueue<String> = LinkedBlockingQueue(100)

}
