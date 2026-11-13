# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon test bootJar && cp build/libs/fieldpilot-1.0.0.jar /workspace/fieldpilot.jar
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --system fieldpilot && useradd --system --gid fieldpilot --create-home fieldpilot
WORKDIR /app
COPY --from=build --chown=fieldpilot:fieldpilot /workspace/fieldpilot.jar app.jar
USER fieldpilot
EXPOSE 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
ENTRYPOINT ["java","-jar","/app/app.jar"]
