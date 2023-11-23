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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )
    }
}
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MetricsEndpointTest {

    @Test
    fun testMetricsRedirect_counter() {

        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8080

        // Realizar la solicitud al endpoint y validar la estructura
        given()
                .`when`()
                .get("/api/stats/metrics/app.metric.redirect_counter")
                .then()
                //.log().all()
                .assertThat()
                .body("name", equalTo("app.metric.redirect_counter"))
                .body("measurements[0].statistic", equalTo("COUNT"))
                .body("measurements[0].value", equalTo(0.0f))
    }

    @Test
    fun testMetricsURI_counter() {

        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8080

        // Realizar la solicitud al endpoint y validar la estructura
        given()
                .`when`()
                .get("/api/stats/metrics/app.metric.uri_counter")
                .then()
                //.log().all()
                .assertThat()
                .body("name", equalTo("app.metric.uri_counter"))
                .body("measurements[0].statistic", equalTo("COUNT"))
                .body("measurements[0].value", equalTo(0.0f))
    }

    @Test
    fun testMetricsjvm_memory_used() {

        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8080

        val response = RestAssured.get("/api/stats/metrics/jvm.memory.used")

        val jsonPathName = response.path<String>("name")
        val jsonPathDescription = response.path<String>("description")
        val jsonPathBaseUnit = response.path<String>("baseUnit")
        val jsonPathMeasurementsStatistic = response.path<String>("measurements[0].statistic")
        val jsonPathMeasurementsValue = response.path<Number>("measurements[0].value")
        val jsonPathAvailableTagsTag = response.path<String>("availableTags[0].tag")
        val jsonPathAvailableTagsValues = response.path<List<String>>("availableTags[0].values")
        val jsonPathAvailableTagsTag2 = response.path<String>("availableTags[1].tag")
        val jsonPathAvailableTagsValues2 = response.path<List<String>>("availableTags[1].values")

        assertEquals("jvm.memory.used", jsonPathName)
        assertEquals("The amount of used memory", jsonPathDescription)
        assertEquals("bytes", jsonPathBaseUnit)
        assertEquals("VALUE", jsonPathMeasurementsStatistic)
        assertTrue(jsonPathMeasurementsValue is Number)
        assertEquals("area", jsonPathAvailableTagsTag)
        assertEquals(listOf("heap", "nonheap"), jsonPathAvailableTagsValues)
        assertEquals("id", jsonPathAvailableTagsTag2)
        assertEquals(
                listOf(
                        "G1 Survivor Space",
                        "Compressed Class Space",
                        "G1 Old Gen",
                        "Metaspace",
                        "CodeCache",
                        "G1 Eden Space"
                ),
                jsonPathAvailableTagsValues2
        )
    }

    @Test
    fun testMetrics_process_cpu_usage() {

        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8080

        val response = RestAssured.get("/api/stats/metrics/process.cpu.usage") // Asegúrate de obtener la respuesta JSON

        val jsonPathName = response.path<String>("name")
        val jsonPathDescription = response.path<String>("description")
        val jsonPathMeasurementsStatistic = response.path<String>("measurements[0].statistic")
        val jsonPathMeasurementsValue = response.path<Number>("measurements[0].value")
        val jsonPathAvailableTags = response.path<List<Any>>("availableTags")

        assertEquals("process.cpu.usage", jsonPathName)
        assertEquals("The \"recent cpu usage\" for the Java Virtual Machine process", jsonPathDescription)
        assertEquals("VALUE", jsonPathMeasurementsStatistic)
        assertTrue(jsonPathMeasurementsValue is Number)
        assertTrue(jsonPathAvailableTags.isEmpty())
    }

}

