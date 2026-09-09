# Stage 1: Build the self-contained Spring Boot JAR (including static frontend assets)
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x backend/mvnw && ./backend/mvnw -f backend/pom.xml clean package -DskipTests

# Stage 2: Minimal runtime container running only the output JAR
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/backend/target/*.jar app.jar
ENV PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
