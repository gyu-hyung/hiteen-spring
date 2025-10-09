import java.io.ByteArrayOutputStream
import java.time.LocalDateTime


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


// Swagger spec 자동 복사
tasks.register<Copy>("copyOpenApiSpec") {
    dependsOn("generateOpenApiDocs")
    from("$buildDir/api-spec/openapi.json")
    into("docs/openapi")
    rename("openapi.json", "openapi-new.json")
    doLast {
        println("✅ Swagger spec copied to docs/openapi/openapi-new.json")
    }
}

// Swagger 변경 이력 자동 생성
tasks.register("updateSwaggerChangeLog") {
    group = "documentation"
    description = "Generate OpenAPI docs and compare with previous version to update changelog"

    dependsOn("copyOpenApiSpec")

    doLast {
        val openapiDir = file("docs/openapi")
        openapiDir.mkdirs()

        val newSpec = file("docs/openapi/openapi-new.json")
        val oldSpec = file("docs/openapi/openapi-latest.json")
        val changelog = file("docs/openapi/changelog.txt")

        if (!oldSpec.exists()) {
            println("⚠️ No previous spec found, creating initial snapshot...")
            newSpec.copyTo(oldSpec, overwrite = true)
            changelog.writeText("Initial API snapshot created.\n")
            return@doLast
        }

        // openapi-diff 실행
        println("🔍 Running openapi-diff...")
        val output = ByteArrayOutputStream()
        exec {
            commandLine("npx", "openapi-diff", oldSpec.path, newSpec.path)
            standardOutput = output
            isIgnoreExitValue = true
        }

        val diffResult = output.toString().ifBlank { "✅ No API differences found." }
        changelog.appendText(
            "\n[${LocalDateTime.now()}]\n$diffResult\n-----------------------------------\n"
        )


        println("📜 Swagger changelog updated at docs/openapi/changelog.txt")

        // 최신 스냅샷 갱신
        newSpec.copyTo(oldSpec, overwrite = true)
        println("✅ openapi-latest.json updated")
    }
}
