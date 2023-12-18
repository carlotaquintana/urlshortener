package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.InfoNotAvailable
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

/**
 * This class handles the generation and retrieval of QR Codes.
 *
 * [createQR] method generates a QR Code based on an ID and a URL.
 *
 * [getQR] method retrieves a pre-existing QR Code using its ID.
 *
 * Note: Before retrieving a QR Code using [getQR], it must be generated
 * in advance using the [createQR] method.
 */
interface QrUseCase {
    fun createQR(id: String, url: String)

    fun getQR(id: String): ByteArray
}

/**
 * Implementation of [QrUseCase].
 */
class QrUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val qrMap: HashMap<String, ByteArray>
) : QrUseCase {

    override fun createQR(id: String, url: String) {
        shortUrlRepository.findByKey(id)?.let {
            qrMap.getOrPut(id) {
                val image = ByteArrayOutputStream()
                QRCode(url).render().writeImage(image)
                image.toByteArray()
            }
        } ?: throw RedirectionNotFound(id)
    }

    override fun getQR(id: String): ByteArray =
        shortUrlRepository.findByKey(id)?.let {

            if (it.properties.qr == true) {
                qrMap[id]
            } else {
                throw InfoNotAvailable(id, "QR")
            }

        } ?: throw RedirectionNotFound(id)

}
