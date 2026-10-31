# =====================================================================
#  Etapa 1: compilar el .jar con Maven
#  --platform=$BUILDPLATFORM: esta etapa corre en la arquitectura NATIVA de quien
#  construye (el runner de GitHub es AMD64). Un .jar sirve en cualquier CPU, así que
#  no hace falta compilarlo emulando ARM64 (eso sería mucho más lento).
# =====================================================================
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom: Docker guarda en caché las dependencias y no las vuelve
# a descargar mientras el pom no cambie.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# =====================================================================
#  Etapa 2: imagen final, solo con el JRE (sin Maven ni código fuente)
#  Esta etapa sí se genera para cada plataforma (amd64 y arm64).
# =====================================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crear un grupo y usuario de sistema sin privilegios
RUN addgroup --system springboot && adduser --system --ingroup springboot springboot

# Copiar el binario asignando directamente la propiedad al nuevo usuario
COPY --from=build --chown=springboot:springboot /app/target/eventos-api-bucket-*.jar app.jar

# Cambiar la ejecución al usuario no-root
USER springboot:springboot

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
