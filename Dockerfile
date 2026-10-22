# ---------- Stage 1: BUILD (has Maven + JDK, big) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Copy pom.xml first and download libraries: Docker caches this layer,
# so later builds skip the download unless pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Tests need a database, so they run in CI / on your laptop, not inside the image build.
RUN mvn -q -B package -DskipTests

# ---------- Stage 2: RUN (only a Java runtime + our jar, small) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Never run as root inside a container
RUN useradd --system --uid 1001 appuser
COPY --from=build /src/target/app.jar app.jar
USER appuser
EXPOSE 8080
# MaxRAMPercentage: let Java use 75% of the container's memory (free tier = 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    THYMELEAF_CACHE=true
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
