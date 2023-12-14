package es.unizar.urlshortener

import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
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

    private val logger: Logger = LogManager.getLogger(MetricCapture::class.java)

    @Value("\${metric.endpoint}")
    private lateinit var metricEndpoint: String

    @Suppress("MagicNumber")
    private val tamBloqueante = 10 // Cola bloqueante de tamaño 10

    @Suppress("MagicNumber")
    private val numConsumidores = 2 // Dos procesos consumidores

    // Cola bloqueante para los comandos y ExecutorService para ejecutarlos
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
            val endpoint = "$metricEndpoint$metricName"

            // Realizar la solicitud HTTP para obtener la métrica
            val response = restTemplate.getForObject(endpoint, Map::class.java)

            // Procesa la respuesta para obtener el valor de la métrica
            val measurements = castToMapList(response?.get("measurements"))
            val metricValue = measurements?.firstOrNull()?.get("value")

            logger.info("Metric $metricName: $metricValue")
        }
    }

    // Convierte la respuesta de la solicitud HTTP en una lista de mapas
    private fun castToMapList(value: Any?): List<Map<String, Any>>? {
        return if (value is List<*> && value.all { it is Map<*, *> }) {
            @Suppress("UNCHECKED_CAST")
            value as List<Map<String, Any>>
        } else {
            null
        }
    }

    // Inicia los consumidores de la cola de comandos después de la inicialización
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
