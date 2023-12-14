package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.BlockingQueue

/**
 * Cola para almacenar las URIs que se van a comprobar si son alcanzables o no.
 */
@Component
open class ColaAlcanzable(
    private val colaAlcanzable: BlockingQueue<String>,
    private val reachableURIUseCase: ReachableURIUseCase
)
{

    /**
     * Método que se ejecuta cada 500ms para comprobar si hay URIs en la cola
     * y comprobar si son alcanzables o no. Como tiene la anotación @Async, se ejecuta
     * en un hilo aparte y no bloquea la ejecución del programa.
     */
    @Async("verificarAlcanzabilidad")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        if (!colaAlcanzable.isEmpty()) {
            // Se saca el primer elemento de la cola y se comprueba si es alcanzable
            val uri = colaAlcanzable.take()
            println("Comprobando si $uri es alcanzable...")
            reachableURIUseCase.verifyReachability(uri)
        }
    }
}