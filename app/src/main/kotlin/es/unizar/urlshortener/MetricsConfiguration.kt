package es.unizar.urlshortener

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {
    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry: MeterRegistry ->
            registry.config()
                .meterFilter(MeterFilter.deny { id: Meter.Id ->
                    val name = id.name
                    !("jvm.memory.used" == name || "process.cpu.usage" == name ||
                            "app.metric.redirect_counter" == name || "app.metric.uri_counter" == name)
                })
        }
    }
}
