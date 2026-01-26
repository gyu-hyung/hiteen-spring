plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
//	kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
}

openApi {
    apiDocsUrl.set("http://localhost:8080/api-docs")
    outputDir.set(file("$buildDir/api-spec"))
    outputFileName.set("openapi.json")
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

    testImplementation("io.mockk:mockk:1.13.12")

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
//	implementation("io.r2dbc:r2dbc-postgresql:0.8.13.RELEASE")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")
    runtimeOnly ("org.postgresql:postgresql")

	// Mockito
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

	// Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")

	// MQTT
	implementation("org.apache.camel.springboot:camel-spring-boot-starter:4.8.5")
	implementation("org.apache.camel.springboot:camel-paho-mqtt5-starter:4.8.5")

	// Reactive Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Reactive Cache
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Mongo DB
//	implementation("org.springframework.data:spring-data-mongodb:4.3.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
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

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    //firebase
    implementation("com.google.firebase:firebase-admin:9.2.0")

    //DB migration
    implementation("org.liquibase:liquibase-core")

    //admob
    implementation("com.nimbusds:nimbus-jose-jwt:9.31")

    //actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    //prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

    //poi
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    //asset
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.drewnoakes:metadata-extractor:2.19.0") // (선택) exif 관련 보조

    // ImageIO 확장(서버 환경에서 JPEG/CMYK/Progressive 등 포맷 읽기 안정화)
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.12.0")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.12.0")
    implementation("com.twelvemonkeys.imageio:imageio-metadata:3.12.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")

    // WebP ImageIO writer(썸네일 webp 출력용)
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
}


kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}


tasks.test {
	useJUnitPlatform()
    jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
}
