package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.usecases.QrUseCaseImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.BlockingQueue

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