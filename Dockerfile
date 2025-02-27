# Étape 1 : Construire l'application avec Gradle
FROM gradle:jdk23 AS builder
WORKDIR /app

# Copier tous les fichiers du projet
COPY . .

# Construire l'application en ignorant les tests
RUN gradle build -x test --no-daemon

# Étape 2 : Utiliser une image plus légère pour exécuter l'application
FROM eclipse-temurin:23-jre
WORKDIR /app

# Copier le JAR généré depuis l'étape précédente
COPY --from=builder /app/build/libs/*.jar app.jar

# Exposer le port 8080
EXPOSE 8080

# Exécuter l'application automatiquement
ENTRYPOINT ["java", "-jar", "app.jar"]