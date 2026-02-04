## -----------------------------
## Runtime stage only
## -----------------------------
#FROM eclipse-temurin:17-jre
#
#WORKDIR /app
#
## 환경변수로 프로파일 설정 (기본값: prod)
#ENV SPRING_PROFILES_ACTIVE=prod
#
## spring 유저 생성 및 /app 권한 세팅
#RUN useradd -m -u 10001 -d /home/spring spring && \
#    mkdir -p /app/assets && \
#    chown -R spring:spring /app && \
#    chmod -R 755 /app
#
## 빌드 시점에 생성된 실행 가능한 JAR만 복사
#COPY build/libs/hiteen-0.0.1-SNAPSHOT.jar app.jar
#
#USER spring
#
#EXPOSE 8080
#
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]











# -----------------------------
# 1️⃣ Build stage
# -----------------------------
FROM gradle:8.9-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test -Pspring.profiles.active=dev-k8s

# -----------------------------
# 2️⃣ Runtime stage
# -----------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# spring 유저 생성 및 /app 권한 세팅
RUN useradd -m -u 10001 -d /home/spring spring && \
    mkdir -p /app/assets && \
    chown -R spring:spring /app && \
    chmod -R 755 /app

# JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=dev-k8s", "-jar", "/app/app.jar"]
