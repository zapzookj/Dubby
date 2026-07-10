# ── build stage ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true
COPY src ./src
RUN ./gradlew --no-daemon bootJar --quiet

# ── runtime stage ──
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
