# Build with the Maven wrapper so local and container builds use the same project configuration.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -ntp -DskipTests dependency:resolve
COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S -g 10001 ledgerflow && adduser -S -D -H -u 10001 -G ledgerflow ledgerflow
COPY --from=build --chown=10001:10001 /workspace/target/ledgerflow-*.jar /app/ledgerflow.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/ledgerflow.jar"]
