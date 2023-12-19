@file:Suppress("MatchingDeclarationName", "WildcardImport")

package es.unizar.urlshortener

import com.jayway.jsonpath.JsonPath
import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.RestAssured.port
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.isA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI
import java.util.concurrent.TimeUnit

@Suppress("UnusedPrivateProperty")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        TimeUnit.SECONDS.sleep(2L)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://example.com/")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "ftp://example.com/"

        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    private fun shortUrl(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["qr"] = "false"

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )
    }

}
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MetricsEndpointTest {

    @LocalServerPort
    private val localServerPort = 0

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = localServerPort
    }

    @Test
    fun testMetricsRedirect_counter() {

        // Realizar la solicitud al endpoint y validar la estructura
        given().log().all()
            .`when`()
            .get("/api/stats/metrics/app.metric.redirect_counter")
            .then()
            .assertThat()
            .body("name", equalTo("app.metric.redirect_counter"))
            .body("measurements[0].statistic", equalTo("VALUE"))
    }

    @Test
    fun testMetricsURI_counter() {


        // Realizar la solicitud al endpoint y validar la estructura
        given()
            .`when`()
            .get("/api/stats/metrics/app.metric.uri_counter")
            .then()
            .log().all()
            .assertThat()
            .body("name", equalTo("app.metric.uri_counter"))
            .body("measurements[0].statistic", equalTo("VALUE"))
    }

    @Test
    fun testMetricsjvm_memory_used() {

        given()
            .log().all()
            .`when`()
            .get("/api/stats/metrics/jvm.memory.used")
            .then()
            .log().all()
            .assertThat()
            .body("name", equalTo("jvm.memory.used"))
            .body("description", equalTo("The amount of used memory"))
            .body("measurements[0].statistic", equalTo("VALUE"))
    }

    @Test
    fun `testMetrics process cpu usage`() {

        given()
            .log().all()
            .`when`()
            .get("/api/stats/metrics/process.cpu.usage")
            .then()
            .log().all()
            .assertThat()
            .body("name", equalTo("process.cpu.usage"))
            .body("description", equalTo("The \"recent cpu usage\" for the Java Virtual Machine process"))
            .body("measurements[0].statistic", equalTo("VALUE"))
    }



}

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class QRTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    private val localServerPort = 0

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = localServerPort
    }

    private fun createShortUrlWithQR(originalUrl: String): ResponseEntity<ShortUrlDataOut> {
        val requestHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("url", originalUrl)
            add("qr", "true")
        }

        val apiUrl = "http://localhost:$port/api/link"
        val requestEntity = HttpEntity(requestBody, requestHeaders)

        return restTemplate.postForEntity(apiUrl, requestEntity, ShortUrlDataOut::class.java)
    }

    private fun callQR(url: String): ResponseEntity<ByteArrayResource> {
        return restTemplate.getForEntity(url, HttpHeaders(), ByteArrayResource::class.java)
    }

    @Test
    fun `test creating a short URL returns a basic redirect with QR code`() {
        // Act
        val responseEntity = createShortUrlWithQR("http://example.com")

        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(responseEntity.headers.location).isEqualTo(URI.create("http://localhost:$port/bf19bedb"))
        assertThat(responseEntity.body?.url).isEqualTo(URI.create("http://localhost:$port/bf19bedb"))
        assertThat(responseEntity.body?.properties?.get("qr")).isEqualTo("http://localhost:$port/bf19bedb/qr")

        // Verify database state
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `test QR endpoint returns an image when the key exists`() {
        // Arrange
        createShortUrlWithQR("http://example.com")
        TimeUnit.SECONDS.sleep(2L)

        // Act
        val qrCodeResponse = callQR("http://localhost:$port/bf19bedb/qr")

        // Assert
        assertThat(qrCodeResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(qrCodeResponse.body).isNotNull
    }

    /*@Test
    fun `test QR endpoint returns not found when the key does not exist`() {
        // Act
        val response = callQR("http://localhost:$port/bf19bedb/qr")

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test QR endpoint returns a bad request when the key exists but no QR code is available`() {
        // Arrange
        createShortUrlWithQR("http://example.com")
        TimeUnit.SECONDS.sleep(2L)

        //Act
        val response = callQR("http://localhost:$port/bf19bedb/qr")

        //Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }*/


}

