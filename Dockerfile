# Production image for EcoSwap (Java 21 + Spring Boot). Used by Railway, Render, Fly.io or any Docker host.
# Build:  docker build -t ecoswap .
# Run:    docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -v ecoswap-data:/data ecoswap

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -q -B -DskipTests package && cp target/ecoswap-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
# Uploaded photos live here: mount a persistent volume at /data so they survive restarts and redeploys.
RUN mkdir -p /data/storage
ENV SPRING_PROFILES_ACTIVE=prod \
    ECOSWAP_STORAGE_DIR=/data/storage \
    JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=170m -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
# The platform's $PORT is honoured (server.port=${PORT:8080}).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
