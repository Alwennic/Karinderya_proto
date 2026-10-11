# ---- base: Maven + JDK, dependencies downloaded once ----
FROM maven:3.9-eclipse-temurin-21 AS base
WORKDIR /app
COPY pom.xml .
# pre-download dependencies so later builds are cached (ignore errors, they are fetched again later if needed)
RUN mvn -B -q dependency:go-offline || true
COPY src ./src

# ---- test stage: Jenkins runs "docker build --target test" ----
FROM base AS test
RUN mvn -B test

# ---- build stage: package the jar without re-running tests ----
FROM base AS build
RUN mvn -B -q -DskipTests package

# ---- production stage: small JRE image, only the jar, non-root user ----
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app
COPY --from=build /app/target/catalog-api.jar app.jar
EXPOSE 5000
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s CMD wget -qO- http://localhost:5000/health || exit 1
USER nobody
CMD ["java", "-jar", "app.jar"]
