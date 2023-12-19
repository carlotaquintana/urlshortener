@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.concurrent.BlockingQueue
import org.mockito.BDDMockito.*
import java.time.OffsetDateTime


@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var reachableURIUseCase: ReachableURIUseCase


    @MockBean
    private lateinit var qrUseCase: QrUseCase

    @MockBean
    private lateinit var limitUseCase: LimitUseCase

    @Suppress("UnusedPrivateMember")
    @MockBean
    private lateinit var reachableQueue: BlockingQueue<String>

    @Suppress("UnusedPrivateMember")
    @MockBean
    private lateinit var qrQueue: BlockingQueue<Pair<String, String>>



    // Se ha modificado para hacer que la URI sea alcanzable (sino devolvía un 403 en
    // vez de un 307).
    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val reachableUrl = "http://example.com/"
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection(reachableUrl))
        given(reachableURIUseCase.reachable(reachableUrl)).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))

            // Se espera que la respuesta sea 307 Temporary Redirect
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl(reachableUrl))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }


    //Test para comprobar que se devuelve un QR
    @Test
    fun `creates returns a basic redirect if it can compute a hash with qr`() {
        val shortUrl =  ShortUrl(
                      "f684a3c4",
                            Redirection("http://example.com/"),
                            OffsetDateTime.now(),
                            ShortUrlProperties(ip = "127.0.0.1", qr = true)
                        )
        given(
            createShortUrlUseCase.create(
                    url = "http://example.com/",
                    data = ShortUrlProperties(ip = "127.0.0.1", sponsor = null, qr = true, limit = null)
            )
        ).willReturn(shortUrl)

        given(reachableQueue.offer("http://example.com/")).willReturn(true)

        assertNotNull(shortUrl)

        mockMvc.perform(
            post("/api/link")
                    .param("url", "http://example.com/")
                    .param("qr", "true")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(
                content().json(
                        """
                            {
                              "url": "http://localhost/f684a3c4",
                              "properties": {
                                "qr": "http://localhost/f684a3c4/qr"
                              }
                            }
                        """.trimIndent()
                    )
                )

    }


    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        given(reachableURIUseCase.reachable("ftp://example.com/")).willReturn(true)

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
    }

    // Tests para comprobar alcanzabilidad y límites--------------------------------------------------------------------
    @Test
    fun `create returns 400 bad request if URI is not reachable`() {
        // Dada una URI no alcanzable
        val unreachableUrl = "http://unreachable-url.com/"
        given(reachableURIUseCase.reachable(unreachableUrl)).willReturn(false)

        // Se hace un POST
        mockMvc.perform(
            post("/api/link")
                .param("url", unreachableUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )

            // Se espera un error 400 Bad Request
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `redirectTo returns 429 too many requests if the key exists but the limit is exceeded`() {
        given(limitUseCase.limitExceeded("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTooManyRequests)
    }

    @Test
    fun `redirectTo returns 404 not found if the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isNotFound)
    }


    /****************************************************************************************
     * Test para la comprobación de QR
     ****************************************************************************************/
    @Test
    fun `generateQR returns a QR when the key exists`() {
        given(qrUseCase.getQR("key")).willReturn(byteArrayOf(0, 1, 2, 3))

        mockMvc.perform(get("/{id}/qr", "key"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(byteArrayOf(0, 1, 2, 3)))
    }

    @Test
    fun `generateQR returns a not found when the key does not exist`() {
        given(qrUseCase.getQR("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}/qr", "key"))
            .andDo(print())
    }

    @Test
    fun `generateQR returns forbidden when the key exists but the qr is invalid`() {
        given(qrUseCase.getQR("key"))
                .willAnswer { throw NotAvailable("key", "QR") }

        mockMvc.perform(get("/{id}/qr", "key"))
                .andDo(print())
    }

    /****************************************************************************************/
}
