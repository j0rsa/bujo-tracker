import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kotlinVersion = "1.3.61"
val config4kVersion = "0.4.1"
val arrowVersion = "0.10.4"
val vertxVersion = "3.8.5"
val junitJupiterEngineVersion = "5.4.2"

val mainVerticleName = "com.j0rsa.bujo.tracker.App"
val watchForChange = "src/**/*"
val doOnChange = "./gradlew classes"

plugins {
	java
	application
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

	implementation("io.vertx:vertx-health-check:$vertxVersion")
	implementation("io.vertx:vertx-web:$vertxVersion")
	implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
	implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
	implementation("com.google.code.gson:gson:2.7")

	implementation("org.jetbrains.exposed:exposed:0.17.7")
	implementation("io.jsonwebtoken:jjwt:0.9.1")
	implementation("org.slf4j:slf4j-api:1.7.25")
	implementation("ch.qos.logback:logback-classic:1.2.3")
	implementation("ch.qos.logback:logback-core:1.2.3")

	testImplementation("io.mockk:mockk:1.9.3")
	testImplementation("io.vertx:vertx-junit5:$vertxVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterEngineVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterEngineVersion")
	testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.20")
	testImplementation("org.jetbrains.spek:spek-api:1.1.5")
	testImplementation("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
	mainClassName = "io.vertx.core.Launcher"
}

tasks {
	named<ShadowJar>("shadowJar") {
		mergeServiceFiles()
		manifest {
			attributes(mapOf("Main-Verticle" to mainVerticleName))
		}
		mergeServiceFiles {
			include("META-INF/services/io.vertx.core.spi.VerticleFactory")
		}
	}
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}

tasks {
	getByName<JavaExec>("run") {
		args = listOf("run", mainVerticleName, "--redeploy=${watchForChange}", "--launcher-class=${application.mainClassName}", "--on-redeploy=${doOnChange}")
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