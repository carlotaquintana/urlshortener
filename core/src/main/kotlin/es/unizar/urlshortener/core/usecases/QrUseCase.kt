package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.InfoNotAvailable
import es.unizar.urlshortener.core.QrService
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

interface QrUseCase {
    //fun generateQR(id: String, url: String)

    fun getQR(id: String): ByteArray
}

class QrUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val qrMap: HashMap<String, ByteArray>,
        //private val qrService: QrService
) : QrUseCase {

    /*override fun generateQR(id: String, url: String) {
        shortUrlRepository.findByKey(id)?.let {
            println("ID generate QR")
            println(id)
            qrMap.put(id, qrService.getQr(url))
        } ?: throw RedirectionNotFound(id)
    }*/

    override fun getQR(id: String): ByteArray =
        shortUrlRepository.findByKey(id)?.let {
            /*val image = ByteArrayOutputStream()

            QRCode(url).render().writeImage(image)
            return image.toByteArray()*/
            if (it.properties.qr == true) {
                /*val image = ByteArrayOutputStream()

                QRCode(url).render().writeImage(image)
                return image.toByteArray()*/
                println("Dentro del if")
                println(id)
                qrMap[id]
            } else {
                throw InfoNotAvailable(id, "QR")
            }

        } ?: throw RedirectionNotFound(id)

}
