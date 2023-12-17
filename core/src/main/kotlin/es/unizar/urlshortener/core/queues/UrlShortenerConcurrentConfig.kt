package es.unizar.urlshortener.core.queues

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Configuración para ejecutar los procesos asíncronos de forma concurrente.
 */
@Configuration
@EnableAsync
@EnableScheduling
@Suppress("NewLineAtEndOfFile")
open class UrlShortenerConcurrentConfig {

    @Bean("configuracionConcurrente")
    open fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.initialize()
        return executor
    }
}
