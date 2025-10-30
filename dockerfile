# 1️⃣ 빌드 단계 (Gradle로 JAR 생성)
FROM gradle:8.9-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test -Pspring.profiles.active=dev

# 2️⃣ 실행 단계 (경량 JRE 이미지)
FROM eclipse-temurin:17-jre
WORKDIR /app

# 보안상 일반 사용자로 실행
RUN useradd -u 10001 spring
USER spring

# 빌드 결과 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 서버 포트 오픈 (Spring Boot 기본 8080)
EXPOSE 8080

# 컨테이너 시작 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
