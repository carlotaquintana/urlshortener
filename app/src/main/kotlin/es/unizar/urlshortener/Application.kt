package es.unizar.urlshortener

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
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
class MetricCapture(private val restTemplate: RestTemplate) {

    @Suppress("MagicNumber")
    private val tamBloqueante = 10 // Cola bloqueante de tamaño 10

    @Suppress("MagicNumber")
    private val numConsumidores = 2 // Dos procesos consumidores


    private val queue = ArrayBlockingQueue<Callable<Unit>>(tamBloqueante)
    private val executorService = Executors.newFixedThreadPool(numConsumidores)

    @Scheduled(fixedRate = 5000)
    fun captureMetricsAsync() {
        val metricNames = listOf(
                "process.cpu.usage",
                "jvm.memory.used",
                "app.metric.uri_counter",
                "app.metric.redirect_counter"
        )

        // Inicia la recolección de métricas para cada nombre en metricNames
        metricNames.forEach { metricName ->
            // Agrega el comando a la cola
            queue.offer(createMetricCalculationCommand(metricName))
        }
    }

    private fun createMetricCalculationCommand(metricName: String): Callable<Unit> {
        return Callable {
            // Construir la URL para la métrica específica
            val endpoint = "http://localhost:8080/api/stats/metrics/$metricName"

            // Realizar la solicitud HTTP para obtener la métrica
            val response = restTemplate.getForObject(endpoint, Map::class.java)

            val measurements = castToMapList(response?.get("measurements"))
            val metricValue = measurements?.firstOrNull()?.get("value")

            println("Metric $metricName: $metricValue")
        }
    }

    private fun castToMapList(value: Any?): List<Map<String, Any>>? {
        return if (value is List<*> && value.all { it is Map<*, *> }) {
            @Suppress("UNCHECKED_CAST")
            value as List<Map<String, Any>>
        } else {
            null
        }
    }

    @Suppress("unused")
    @PostConstruct
    private fun consumeQueue() {
        repeat(2) {
            executorService.submit {
                while (true) {
                    // Toma un comando de la cola (bloqueante)
                    val command = queue.take()
                    // Ejecuta el comando (cálculo de métrica)
                    command.call()
                }
            }
        }
    }
}
