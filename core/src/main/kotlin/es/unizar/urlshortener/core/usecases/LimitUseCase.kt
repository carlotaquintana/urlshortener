package es.unizar.urlshortener.core.usecases

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket;
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

interface LimitUseCase {
    /**
     * Añade una nueva redirección.
     * @param hash hash de la url
     * @param limite limite de redirecciones
     */
    fun newRedirect(hash: String, limite: Int)

    /**
     * Comprueba si se ha superado el límite de redirecciones.
     * @param hash hash de la url
     * @return true si se ha superado el límite, false en caso contrario
     */
    fun limitExceeded(hash: String): Boolean

    /**
     * Obtiene información del límite y número de redirecciones para un hash dado.
     * @param hash hash de la url
     * @return Pair con el límite y número de redirecciones (null si no se encuentra el hash)
     */
    fun getLimitAndRedirections(hash: String): Pair<Int, Long>?
}

/**
 * Tipo de dato para representar una redirección con su hash, bucket y límite.
 */
typealias redirection = Triple<String, Bucket, Int>

/**
 * Implementación de [LimitUseCase].
 */
class LimitUseCaseImpl: LimitUseCase {

    // Lista de redirecciones
    private val redirectionList: MutableList<redirection> = mutableListOf()
    private val logger: Logger = LogManager.getLogger(LimitUseCaseImpl::class.java)

    /**
     * Añade una nueva redirección.
     * @param hash hash de la url
     * @param limite limite de redirecciones
     */
    override fun newRedirect(hash: String, limite: Int) {
        // Si vale 0 ó menos no se establece límite
        if(limite > 0){
            // Se crea un límite de tipo Bandwidth para poder crear el bucket
            val limit: Bandwidth = Bandwidth.builder()
                .capacity(limite.toLong())
                .refillIntervally(limite.toLong(), java.time.Duration.ofHours(1))
                .build()

            logger.info("Se ha creado un bucket con un límite de $limite redirecciones por hora")

            // Se crea un bucket para la url
            val bucket: Bucket = Bucket.builder()
                .addLimit(limit)
                .build()

            // Se crea la redireccion con el hash, el bucket y el límite
            val redirection = Triple(hash, bucket, limite)

            // Se añade la redireccion a la lista como un objeto de tipo URL (hash, bucket y límite)
            redirectionList.add(redirection)
        }
    }

    /**
     * Comprueba si se ha superado el límite de redirecciones.
     * @param hash hash de la url
     * @return true si se ha superado el límite, false en caso contrario
     */
    override fun limitExceeded(hash: String): Boolean {
        var exceeded = false
        // Se busca el hash en la lista de redirecciones
        for (redirection in redirectionList) {
            // Si se encuentra el hash
            if (redirection.first == hash) {
                // Se comprueba si se ha superado el límite
                if(redirection.third.toLong() == redirection.second.availableTokens) {
                    // Si se ha superado el límite se devuelve true
                    exceeded = true
                }
            }
        }
        return exceeded
    }

    override fun getLimitAndRedirections(hash: String): Pair<Int, Long>? {
        val redirection = redirectionList.find { it.first == hash }
        return redirection?.let { Pair(it.third, it.second.availableTokens) }
    }
}
