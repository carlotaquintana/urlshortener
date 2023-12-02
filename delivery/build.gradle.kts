plugins {
    id("urlshortener.spring-library-conventions")
    kotlin("plugin.spring")
}

repositories{
    mavenCentral()
}


dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("commons-validator:commons-validator:${Version.COMMONS_VALIDATOR}")
    implementation("com.google.guava:guava:${Version.GUAVA}")
    implementation("io.github.bucket4j:bucket4j-core:4.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Version.MOCKITO}")
}
