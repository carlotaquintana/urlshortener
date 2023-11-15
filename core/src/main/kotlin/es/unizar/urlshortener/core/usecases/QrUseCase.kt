package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

interface QrUseCase {
    fun generateQR(id: String, url: String): ByteArray
}

class QrUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : QrUseCase {

    override fun generateQR(id: String, url: String) : ByteArray {
        shortUrlRepository.findByKey(id)?.let {
            val image = ByteArrayOutputStream()

            QRCode(url).render().writeImage(image)
            return image.toByteArray()

        } ?: throw RedirectionNotFound(id)
    }

}
