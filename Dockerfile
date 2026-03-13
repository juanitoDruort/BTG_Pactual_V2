# ============================================================
# Dockerfile Multi-Stage - BTG Pactual V2
# ============================================================
# Stage 1: Build con Gradle + JDK 24
# Stage 2: Runtime minimal con JRE 24
# ============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:24-jdk AS build

WORKDIR /app

# Copiar archivos de Gradle primero (cacheo de dependencias)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Dar permisos de ejecución a gradlew
RUN chmod +x gradlew

# Descargar dependencias (capa cacheada)
RUN ./gradlew dependencies --no-daemon || true

# Copiar código fuente
COPY src/ src/

# Construir JAR (sin ejecutar tests)
RUN ./gradlew bootJar --no-daemon -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:24-jre

# Instalar curl para healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# Copiar JAR desde build stage (renombrado para evitar wildcard)
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
