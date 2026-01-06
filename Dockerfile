# -----------------------------
# Runtime stage only
# -----------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# spring 유저 생성 및 /app 권한 세팅
RUN useradd -m -u 10001 -d /home/spring spring && \
    mkdir -p /app/assets && \
    chown -R spring:spring /app && \
    chmod -R 755 /app

# ✅ CI에서 빌드된 JAR만 복사
COPY build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=dev-k8s", "-jar", "/app/app.jar"]
