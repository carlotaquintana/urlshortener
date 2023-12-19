package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.usecases.QrUseCaseImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.BlockingQueue

/**
 * Component responsible for asynchronously processing a queue of QR Code generation requests.
 * It uses a blocking queue (`qrQueue`) to store pairs of IDs and URLs.
 * The `executor` function, annotated with `@Async` and scheduled at a fixed delay, continuously checks
 * the queue and generates QR Codes using the injected `qrUseCase` if there are pending requests.
 *
 * @param qrQueue Blocking queue for storing pairs of IDs and URLs to be processed.
 * @param qrUseCase Implementation of the QR Code generation use case.
 */
@Component
open class QRCola(
        private val qrQueue: BlockingQueue<Pair<String, String>>,
        private val qrUseCase: QrUseCaseImpl
) {
    private val logger: Logger = LogManager.getLogger(QRCola::class.java)
    @Async("configuracionConcurrente")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        if (!qrQueue.isEmpty()) {
            val result = qrQueue.take()
            logger.info("Generating QR for ${result.first}")
            qrUseCase.createQR(result.first, result.second)
        }
    }
}
