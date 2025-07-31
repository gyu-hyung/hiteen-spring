plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "kr.jiasoft"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Postgres
	runtimeOnly("org.postgresql:postgresql")

	// 테스트용 (JUnit5 기반, WebFlux 대응)
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test")

	// Mockito
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

	// WebFlux(리액티브) 서버
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// jpa
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Coroutine
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Kotlin 리플렉션(코틀린 프레임워크 작동에 필요)
	implementation("org.jetbrains.kotlin:kotlin-reflect")


	// MQTT
	implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
	implementation("org.springframework.integration:spring-integration-mqtt")

	// Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.data:spring-data-redis:3.3.0")

	// Mongo DB
	implementation("org.springframework.data:spring-data-mongodb:4.3.0")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

	// JSON
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")


	// JWT
	val jjwtVersion = "0.12.6"
	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")


}


kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}


tasks.test {
	useJUnitPlatform()
}
