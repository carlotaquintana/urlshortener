package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import es.unizar.urlshortener.core.usecases.ReachableURIUseCaseImpl
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * Cola que almacena URLs que se han de verificar si son alcanzables.
 */
class ReachableQueue {
    private val queue: Queue<Any> = ConcurrentLinkedQueue()
    private val reachableUseCase: ReachableURIUseCase = ReachableURIUseCaseImpl()

    /**
     * Se añade un elemento a la cola.
     */
    fun addToQueue(item: Any?) {
        queue.add(item)
    }

    /**
     * Se obtiene un elemento de la cola.
     */
    private fun getFromQueue(): Any {
        return queue.poll()
    }

    /** Se verifica la alcanzabilidad de forma asíncrona. Se crea un nuevo hilo de ejecución en el que
     * se va a estar comprobando la cola de URLs a verificar.
     */
    fun verificarAlcanzabilidad() {
        val thread = Thread(Runnable {
            while (true) {
                val item = getFromQueue()
                if (item != null) {
                    // Verificar alcanzabilidad de la URI
                    if(reachableUseCase.reachable(item.toString())){
                        TODO("Si es alcanzable...")
                    }
                    else{
                        TODO("Si no es alcanzable, ¿enviar 201 a POST?")
                    }
                }
            }
        })
        thread.start()
    }
}