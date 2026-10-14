# Etapa 1: Compilación del proyecto con Maven y Java 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Descarga de dependencias en capa cacheada
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia de código fuente y empaquetado del JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen ligera de ejecución (JRE 17 Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/checkautos-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

# Configuración optimizada de memoria para planes gratuitos (512 MB RAM)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
