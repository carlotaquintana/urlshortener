package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.RedirectCounterService
import es.unizar.urlshortener.infrastructure.delivery.UriCounterService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringBootApplication
@EnableScheduling
class Application {
    @Bean
    fun restTemplate(): RestTemplate {
        // Para realizar solicitudes HTTP
        return RestTemplate()
    }
}

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args)
}

@Component
class MetricCapture(
    private val restTemplate: RestTemplate,
    private val redirectCounterService: RedirectCounterService,
    private val uriCounterService: UriCounterService
) {

    private val executorService1: ExecutorService = Executors.newSingleThreadExecutor()
    private val executorService2: ExecutorService = Executors.newSingleThreadExecutor()

    @Scheduled(fixedRate = 5000)
    fun captureMetricsAsync() {
        val metricNames = listOf(
            "app.metric.uri_counter",
            "app.metric.redirect_counter"
        )

        // Inicia la recolección de métricas para cada nombre en metricNames
        // Agrega un mensaje a la cola para cada nombre en metricNames
        metricNames.forEach { metricName ->
            redirectCounterService.addToQueue(metricName)
            uriCounterService.addToQueue(metricName)
        }
    }


    // Inicia los consumidores de la cola de comandos después de la inicialización
    @Suppress("unused")
    @PostConstruct
    private fun consumeQueue() {
        redirectCounterService.clearQueue()
        uriCounterService.clearQueue()
        // Inicia los consumidores de la cola de comandos
        executorService1.submit { redirectCounterService.compute() }
        executorService2.submit { uriCounterService.compute() }
    }
}