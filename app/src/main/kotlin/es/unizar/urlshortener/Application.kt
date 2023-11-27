package es.unizar.urlshortener

import ch.qos.logback.core.util.SystemInfo
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.FileWriter
import java.io.IOException
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.util.concurrent.CompletableFuture
import org.springframework.web.client.RestTemplate

/**
 * The marker that makes this project a Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling
class Application {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate() // para realizar solicitudes HTTP
    }
}

/**
 * The main entry point.
 */
fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args)
}


@Component
class MetricCapture (private val restTemplate: RestTemplate) {

    private val metricsEndpointUrl = "http://localhost:8080/api/stats/metrics"

    @Scheduled(fixedRate = 5000)
    fun captureMetricsAsync() {
        val metricNames = listOf(
                "process.cpu.usage",
                "jvm.memory.used",
                "app.metric.uri_counter",
                "app.metric.redirect_counter"
        )

        // Inicia la recolección de métricas para cada nombre en metricNames
        val captureMetric = metricNames.map { metricName ->
            captureMetricAsync(metricName)
        }.toTypedArray()

        CompletableFuture.allOf(*captureMetric).join()
    }

    private fun castToMapList(value: Any?): List<Map<String, Any>>? {
        return if (value is List<*> && value.all { it is Map<*, *> }) {
            @Suppress("UNCHECKED_CAST")
            value as List<Map<String, Any>>
        } else {
            null
        }
    }

    @Async
    fun captureMetricAsync(metricName: String): CompletableFuture<Unit> {
        // Construye la URL para la métrica específica
        val endpoint = "$metricsEndpointUrl/$metricName"
        // Realiza una solicitud HTTP para obtener la métrica
        val response = restTemplate.getForObject(endpoint, Map::class.java)

        val measurements = castToMapList(response?.get("measurements"))

        val metricValue = measurements?.firstOrNull()?.get("value")

        println("Metric $metricName: $metricValue")

        return CompletableFuture.completedFuture(Unit)
    }
}













/**
 * Sirve para tema de escalabilidad. Coger ideas de aqui
 */
//@Component
class CustomInfo(private val  meterRegistry: MeterRegistry) : InfoContributor {
    // Se crea el endpoint de info
    override fun contribute(builder: Info.Builder) {
        builder.withDetail("counter", meterRegistry.counter("app.metric.redirect_counter").count())
        builder.withDetail("URIcounter", meterRegistry.counter("app.metric.uri_counter").count())

        val usedMemory = meterRegistry.find("jvm.memory.used")
                .tags("area", "heap")
                .gauge()
                ?.value()

        builder.withDetail("jvm.memory.used", usedMemory)

        // Obtener todos los medidores registrados en el MeterRegistry
        val allMeters: List<Meter> = meterRegistry.meters

        // Iterar sobre cada medidor y escribir detalles en el archivo
        for (meter in allMeters) {
            val meterName = meter.id.name
            val meterType = meter.id.type

            // Agregar detalles al Info.Builder
            builder.withDetail("${meterType}.${meterName}", meter.measure().joinToString(", ") { "${it.value}" })
        }

    }
}
