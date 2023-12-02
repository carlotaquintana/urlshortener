plugins {
    id("urlshortener.kotlin-common-conventions")
}

repositories{
    mavenCentral()
}

dependencies{
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:8.7.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
}