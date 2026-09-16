# =========================
# 1. Build
# =========================
FROM gradle:8.10-jdk21 AS builder

WORKDIR /app

# Copy Gradle configuration first for better layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source
COPY src ./src

# Build application
RUN ./gradlew clean bootJar --no-daemon


# =========================
# 2. Runtime
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]