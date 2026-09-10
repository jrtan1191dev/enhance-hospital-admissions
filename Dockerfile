# Stage 1: Build GraalVM native executable (including static frontend assets)
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /app
COPY . .
RUN chmod +x backend/mvnw && ./backend/mvnw -f backend/pom.xml -Pnative native:compile -DskipTests -DquickBuild=true

# Stage 2: Minimal runtime container running the native executable
FROM debian:bookworm-slim
WORKDIR /app
COPY --from=builder /app/backend/target/admissions app
ENV PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["sh", "-c", "./app --server.port=${PORT}"]

