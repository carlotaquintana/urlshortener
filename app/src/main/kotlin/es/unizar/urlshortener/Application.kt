package es.unizar.urlshortener

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

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
    }
}
