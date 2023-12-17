package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.BlockingQueue


/**
 * Cola para almacenar las URIs que se van a comprobar si son alcanzables o no.
 */
@Component
open class ColaAlcanzable(
    @Qualifier("reachableQueue") private val colaAlcanzable: BlockingQueue<String>,
    private val reachableURIUseCase: ReachableURIUseCase
)
{
    private val logger: Logger = LogManager.getLogger(ColaAlcanzable::class.java)
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
            // problema aqui: la cola se puede desbordar habria algun critero para que no se desborde?
            // le puedes añadir limite, que tena un limite de datos, si la cola esta llena que rechace la redireccion
            // 100 o 200, si se tira se dice que no se recorta
            // con esto memory leak
            // Se saca el primer elemento de la cola y se comprueba si es alcanzable
            val uri = colaAlcanzable.take()
            logger.info("Comprobando si la URI $uri es alcanzable")
            reachableURIUseCase.verifyReachability(uri)
        }
    }
}
