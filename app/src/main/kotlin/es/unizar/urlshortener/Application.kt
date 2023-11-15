package es.unizar.urlshortener

import ch.qos.logback.core.util.SystemInfo
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.io.FileWriter
import java.io.IOException
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

/**
 * The marker that makes this project a Spring Boot application.
 */
@SpringBootApplication
class Application

/**
 * The main entry point.
 */
fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args)
}

@Component
class CustomInfo(val  meterRegistry: MeterRegistry) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        builder.withDetail("counter", meterRegistry.counter("demo_counter").count())
        builder.withDetail("URIcounter", meterRegistry.counter("uri_counter").count())

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
