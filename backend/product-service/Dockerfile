# -----------------------------------------------------------------------------
# product-service — multi-stage production image
# Stack: Spring Boot 4.1 / Java 21. Config: AWS Parameter Store (/obs/prod/), S3.
# Port: 8082. Health: GET /actuator/health (spring-boot-starter-actuator).
# -----------------------------------------------------------------------------

# ---- Build stage: compile the fat JAR inside the image (Jenkins also runs tests) ----
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
WORKDIR /workspace

# Copy only the module contents (build context must be backend/product-service)
COPY pom.xml .
COPY src ./src

# Tests are executed in Jenkins "Unit Tests" stage; skip here to keep image builds fast/deterministic.
# Resolve the repackaged fat JAR dynamically (avoid hardcoding version strings that drift with pom).
RUN mvn -B -DskipTests package \
    && JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" \
    && test -n "$JAR" \
    && cp "$JAR" /workspace/app.jar

# ---- Runtime stage: JRE only (no Maven/JDK) for smaller attack surface ----
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl is required for HEALTHCHECK; cleaned apt cache keeps the layer smaller.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --create-home --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown app:app /app/app.jar

# Non-root: container breakout / accidental host mounts are less catastrophic.
USER app

EXPOSE 8082

# prod profile picks up application-prod.yml; Parameter Store import remains active.
# MaxRAMPercentage lets the JVM use most of the container memory limit instead of host heuristics.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    AWS_REGION=ap-south-1

# start-period covers Spring Boot + Parameter Store + RDS connectivity on cold start.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:8082/actuator/health" >/dev/null || exit 1

# exec keeps PID 1 as the JVM so SIGTERM reaches Spring Boot for graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
