import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("urlshortener.kotlin-common-conventions")
}
dependencies{
    implementation("org.springframework:spring-context:5.3.13")
    implementation("org.apache.logging.log4j:log4j-api:2.22.0")
    implementation("io.github.g0dkar:qrcode-kotlin-jvm:3.2.0")
    implementation("org.springframework:spring-context:5.3.13")
    implementation("org.apache.logging.log4j:log4j-api:2.22.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
}

repositories{
    mavenCentral()
}
