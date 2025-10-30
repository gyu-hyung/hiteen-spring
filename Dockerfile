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

# spring 유저 홈 생성 및 권한 로그 출력
RUN useradd -m -u 10001 -d /home/spring spring && \
    mkdir -p /home/spring/assets && \
    echo "[DEBUG] created /home/spring and /home/spring/assets" && \
    ls -ld /home /home/spring /home/spring/assets && \
    chown -R spring:spring /home/spring && \
    chmod -R 755 /home/spring && \
    echo "[DEBUG] after chown/chmod:" && ls -ld /home/spring /home/spring/assets && \
    echo "[DEBUG] user info:" && id spring

# JAR 복사 (root로)
COPY --from=build /app/build/libs/*.jar app.jar

# 디버깅용: 실제 홈 및 권한 확인 로그 출력
RUN echo "[DEBUG] whoami before USER switch:" && whoami && \
    echo "[DEBUG] /home/spring perms:" && ls -ld /home/spring && \
    echo "[DEBUG] switching to user spring"

# spring 유저로 실행
USER spring

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "echo '[DEBUG] whoami at runtime:' $(whoami) && ls -ld /home/spring && java -Dspring.profiles.active=dev -jar /app/app.jar"]
