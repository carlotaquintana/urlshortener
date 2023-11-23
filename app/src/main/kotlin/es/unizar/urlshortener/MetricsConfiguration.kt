package es.unizar.urlshortener

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

//@Configuration
class MetricsConfiguration {
    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry: MeterRegistry ->
            registry.config()
                    .meterFilter(MeterFilter.deny { id: Meter.Id ->
                        val name = id.name
                        !("metric1" == name || "metric2" == name) // Include only metric1 and metric2
                    })
        }
    }
}