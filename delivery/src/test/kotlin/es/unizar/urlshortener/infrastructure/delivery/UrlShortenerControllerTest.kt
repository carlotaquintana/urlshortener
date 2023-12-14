@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.ReachableURIUseCase
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.Mockito.`when`
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

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        given(reachableURIUseCase.reachable("http://example.com/")).willReturn(true)

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
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
            .andExpect(jsonPath("$.statusCode").value(400))
    }

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
    fun `returns HTTP 403 when URI is not reachable during redirection`() {
        // Dado un id registrado y una URI de destino no alcanzable
        val id = "existing-key"
        val unreachableUrl = "http://unreachable-url.com/"
        given(redirectUseCase.redirectTo(id)).willReturn(Redirection(unreachableUrl))
        given(reachableURIUseCase.reachable(unreachableUrl)).willReturn(false)

        // Se hace un GET
        mockMvc.perform(get("/{id}", id))

            // Se espera un error 403
            .andExpect(status().isForbidden)
    }

}
