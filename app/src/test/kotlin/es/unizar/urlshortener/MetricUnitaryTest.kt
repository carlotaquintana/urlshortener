@file:Suppress("MatchingDeclarationName", "WildcardImport")

package es.unizar.urlshortener

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.infrastructure.delivery.RedirectCounterService
import es.unizar.urlshortener.infrastructure.delivery.UriCounterService
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import java.util.concurrent.BlockingQueue

class RedirectCounterServiceTest {
    private lateinit var meterRegistry: MeterRegistry
    private lateinit var clickRepositoryService: ClickRepositoryService
    private lateinit var reachableQueueMetric: BlockingQueue<String>
    private lateinit var uriCounterService: UriCounterService
    private lateinit var redirectCounterService: RedirectCounterService
    private lateinit var metricCapture: MetricCapture
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService
    private lateinit var uriQueueMetric: BlockingQueue<String>

    @BeforeEach
    fun setUp() {
        meterRegistry = mock(MeterRegistry::class.java)
        clickRepositoryService = mock(ClickRepositoryService::class.java)
        @Suppress("UNCHECKED_CAST")
        reachableQueueMetric = mock(BlockingQueue::class.java) as BlockingQueue<String>
        @Suppress("UNCHECKED_CAST")
        uriQueueMetric = mock(BlockingQueue::class.java) as BlockingQueue<String>
        shortUrlRepositoryService = mock(ShortUrlRepositoryService::class.java)

        // Inicializa RedirectCounterService y UriCounterService
        redirectCounterService = RedirectCounterService(meterRegistry, clickRepositoryService, reachableQueueMetric)
        uriCounterService = UriCounterService(meterRegistry, shortUrlRepositoryService, uriQueueMetric)

        metricCapture = MetricCapture(redirectCounterService, uriCounterService)
    }


    @Test
    fun `test consumer behavior when metrics are added to the queues`() {
        // Given
        val redirectMetric = "app.metric.redirect_counter"
        val uriMetric = "app.metric.uri_counter"

        `when`(reachableQueueMetric.take())
            .thenReturn(redirectMetric)
            .thenReturn(uriMetric)

        `when`(uriQueueMetric.take())
            .thenReturn(redirectMetric)
            .thenReturn(uriMetric)

        `when`(meterRegistry.gauge(eq(redirectMetric), any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val block = invocation.arguments[1] as () -> Double
            block.invoke()
        }

        `when`(meterRegistry.gauge(eq(uriMetric), any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val block = invocation.arguments[1] as () -> Double
            block.invoke()
        }

        // When
        metricCapture.captureMetricsAsync() // Simula que se añaden ambas metricas a ambas colas

        // Then
        // Verificar que se han añadido ambas metricas a ambas colas
        verify(reachableQueueMetric).put(redirectMetric)
        verify(reachableQueueMetric).put(uriMetric)
        verify(uriQueueMetric).put(uriMetric)
        verify(uriQueueMetric).put(redirectMetric)

        // Segunda verificacion de que se han añadido ambas metricas a ambas colas mediante take()
        val firstRedirectMetric = uriQueueMetric.take()
        val firstUriMetric = reachableQueueMetric.take()

        assertEquals(redirectMetric, firstRedirectMetric)
        assertEquals(redirectMetric, firstUriMetric)

        val secondRedirectMetric = uriQueueMetric.take()
        val secondUriMetric = reachableQueueMetric.take()

        assertEquals(uriMetric, secondRedirectMetric)
        assertEquals(uriMetric, secondUriMetric)

    }

}
