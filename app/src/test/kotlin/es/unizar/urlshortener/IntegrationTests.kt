@file:Suppress("MatchingDeclarationName", "WildcardImport")

package es.unizar.urlshortener

import com.jayway.jsonpath.JsonPath
import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import io.restassured.RestAssured
import io.restassured.RestAssured.given
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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI

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

