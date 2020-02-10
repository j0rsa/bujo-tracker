import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kotlinVersion = "1.3.61"
val config4kVersion = "0.4.1"
val http4kVersion = "3.226.0"
val arrowVersion = "0.10.4"

plugins {
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    id("com.github.ben-manes.versions") version "0.26.0"
    id("com.adarshr.test-logger") version "2.0.0"
    id("com.gorylenko.gradle-git-properties") version "1.4.17"
    id("com.avast.gradle.docker-compose") version "0.9.4"
    id("com.palantir.docker") version "0.24.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.j0rsa.bujo"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    jcenter()
    maven { url = URI("http://dl.bintray.com/kotlin/exposed") }
}

dependencies {
    implementation("org.flywaydb:flyway-core:6.2.2")
    implementation("org.postgresql:postgresql:42.2.9")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("io.arrow-kt:arrow-fx:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")
    implementation("io.github.config4k:config4k:$config4kVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-jetty:$http4kVersion")
    implementation("org.http4k:http4k-format-gson:$http4kVersion")
    implementation("org.http4k:http4k-client-apache:$http4kVersion")
    implementation("org.jetbrains.exposed:exposed:0.17.7")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")

    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.20")
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.j0rsa.bujo.tracker.MainKt"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
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
val baseDockerName = "j0rsa/${project.name}"
val taggedDockerName = "$baseDockerName:$projectTag"

val baseDockerFile = file("$projectDir/Dockerfile")
docker {
    val shadowJar: ShadowJar by tasks
    name = taggedDockerName
    setDockerfile(baseDockerFile)
    tag("DockerTag", "$baseDockerName:latest")
    buildArgs(mapOf("JAR_FILE" to shadowJar.archiveFileName.get()))
    files(shadowJar.outputs)
}
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}