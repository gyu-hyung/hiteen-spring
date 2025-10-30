# -----------------------------
# 1️⃣ Build stage
# -----------------------------
FROM gradle:8.9-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test -Pspring.profiles.active=dev

# -----------------------------
# 2️⃣ Runtime stage
# -----------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# spring 유저 생성 및 /app/assets 권한 세팅
RUN useradd -m -u 10001 -d /home/spring spring && \
    mkdir -p /app/assets && \
    chown -R spring:spring /app && \
    chmod -R 755 /app && \
    echo "[DEBUG] created /app/assets and assigned to spring" && \
    ls -ld /app /app/assets && id spring

# JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# spring 유저로 실행
USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "echo '[DEBUG] whoami:' $(whoami) && ls -ld /app /app/assets && java -Dspring.profiles.active=dev -jar /app/app.jar"]
