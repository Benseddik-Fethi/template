# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copier les fichiers de configuration Maven pour cache des dépendances
COPY pom.xml ./
COPY .mvn/ .mvn/

# Télécharger les dépendances (layer cached séparément)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src/ ./src/

# Builder l'application (skip tests pour build Docker rapide)
RUN mvn clean package -DskipTests -B && \
    mv target/*.jar app.jar

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Métadonnées
LABEL maintainer="Fethi Benseddik"
LABEL description="Production-ready Spring Boot API with OAuth2 and RustFS"
LABEL version="0.0.1-SNAPSHOT"

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis le stage de build
COPY --from=builder /app/app.jar /app/app.jar

# Changer le propriétaire des fichiers
RUN chown -R spring:spring /app

# Créer le répertoire de logs
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

# Basculer vers l'utilisateur non-root
USER spring:spring

# Exposer le port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod

# Démarrer l'application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
