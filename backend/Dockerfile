FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle wrapper (의존성 레이어 캐시)
COPY gradlew .
COPY gradle/ gradle/
RUN chmod +x gradlew

# 빌드 스크립트만 먼저 복사 → 소스 변경 시 의존성 재다운로드 방지
COPY settings.gradle .
COPY build.gradle .
COPY backend/build.gradle backend/
RUN ./gradlew :backend:dependencies --no-daemon -q

# 소스 빌드
COPY backend/src backend/src
RUN ./gradlew :backend:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
