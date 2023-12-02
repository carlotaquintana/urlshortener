package es.unizar.urlshortener.core.usecases

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket;

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

    /**
     * Añade una nueva redirección.
     * @param hash hash de la url
     * @param limite limite de redirecciones
     */
    override fun newRedirect(hash: String, limite: Int) {
        // Se crea un límite de tipo Bandwidth para poder crear el bucket
        val limit: Bandwidth = Bandwidth.builder()
            .capacity(limite.toLong())
            .refillIntervally(limite.toLong(), java.time.Duration.ofMinutes(1))
            .build()

        // Se crea un bucket para la url
        val bucket: Bucket = Bucket.builder()
            .addLimit(limit)
            .build()

        // Se crea la redireccion con el hash, el bucket y el límite
        val redirection = Triple(hash, bucket, limite)
        // Se añade la redireccion a la lista como un objeto de tipo URL (hash, bucket y límite)
        redirectionList.add(redirection)
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
}