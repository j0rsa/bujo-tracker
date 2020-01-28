import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kotlinVersion = "1.3.61"
val config4kVersion = "0.4.1"
val http4kVersion = "3.226.0"
val log4jVersion = "2.12.1"
val jacksonVersion = "2.10.0"
val jaxbVersion = "2.3.0"
val arrowVersion = "0.10.4"

plugins {
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    id("com.github.ben-manes.versions") version "0.26.0"
    id("com.adarshr.test-logger") version "2.0.0"
    id("com.gorylenko.gradle-git-properties") version "1.4.17"
    id("com.avast.gradle.docker-compose") version "0.9.4"
    id("com.palantir.docker") version "0.22.0"
    application
}

application {
    mainClassName = "tracker.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = URI("http://dl.bintray.com/kotlin/exposed") }
}

dependencies {
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("io.arrow-kt:arrow-fx:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")
    implementation("io.github.config4k:config4k:$config4kVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbVersion")
    implementation("com.sun.xml.bind:jaxb-core:$jaxbVersion")
    implementation("com.sun.xml.bind:jaxb-impl:$jaxbVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-jetty:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.http4k:http4k-client-apache:$http4kVersion")
    implementation("org.jetbrains.exposed:exposed:0.14.1")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("com.h2database:h2:1.4.198")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:$jacksonVersion")

    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.20")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"

val hash = Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().use { it.readText() }.trim()
val projectTag = hash
val jarName = "${project.name}-$version.jar"
val baseDockerName = "j0rsa/${project.name}"
val taggedDockerName = "$baseDockerName:$projectTag"

val baseDockerFile = file("$projectDir/Dockerfile")
docker {
    name = taggedDockerName
    buildArgs(mapOf("JAR_NAME" to jarName))
    setDockerfile(baseDockerFile)
    val jar: Jar by tasks
    files(jar.outputs)
}