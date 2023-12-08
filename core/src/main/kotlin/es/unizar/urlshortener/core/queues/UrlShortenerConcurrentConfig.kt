package es.unizar.urlshortener.core.queues

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
@EnableScheduling
open class UrlShortenerConcurrentConfig {
    @Bean("verificarAlcanzabilidad")
    open fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.initialize()
        return executor
    }

}