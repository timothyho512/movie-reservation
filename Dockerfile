FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

USER 1001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
