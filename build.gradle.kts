plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
//	kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.0"
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

	// 테스트용 (JUnit5 기반, WebFlux 대응)
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test")

	// Spring Security
	implementation("org.springframework.boot:spring-boot-starter-security")

	// WebFlux(리액티브) 서버
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// Coroutine
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Kotlin 리플렉션(코틀린 프레임워크 작동에 필요)
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// R2dbc
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

	// Postgres
	implementation("io.r2dbc:r2dbc-postgresql:0.8.13.RELEASE")

	// Mockito
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

	// Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")

	// MQTT
	implementation("org.apache.camel.springboot:camel-spring-boot-starter:4.8.5")
	implementation("org.apache.camel.springboot:camel-paho-mqtt5-starter:4.8.5")

	// Reactive Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

	// Mongo DB
	implementation("org.springframework.data:spring-data-mongodb:4.3.0")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

	// JSON
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Soketi
    implementation("com.pusher:pusher-http-java:1.3.4")
//    implementation("com.pusher:pusher-http-java:1.2.2")

	// JWT
	val jjwtVersion = "0.12.6"
	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	// Bean Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

    // Aop
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation(project(":coroutine-eloquent"))

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    //firebase
    implementation("com.google.firebase:firebase-admin:9.2.0")

}


kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}


tasks.test {
	useJUnitPlatform()
}
