# -----------------------------
# Runtime stage only
# -----------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# 환경변수로 프로파일 설정 (기본값: prod)
ENV SPRING_PROFILES_ACTIVE=prod

# spring 유저 생성 및 /app 권한 세팅
RUN useradd -m -u 10001 -d /home/spring spring && \
    mkdir -p /app/assets && \
    chown -R spring:spring /app && \
    chmod -R 755 /app

# 빌드 시점에 생성된 실행 가능한 JAR만 복사
COPY build/libs/hiteen-0.0.1-SNAPSHOT.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]