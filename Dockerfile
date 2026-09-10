# Stage 1: Build static frontend assets in isolated Node container
FROM node:24-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build GraalVM native executable with strict memory & parallelism limits (fits within 8GB limit)
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /app
COPY . .
COPY --from=frontend-builder /app/backend/target/classes/static /app/backend/src/main/resources/static
ENV MAVEN_OPTS="-Xmx1g -XX:+UseSerialGC"
RUN chmod +x backend/mvnw && ./backend/mvnw -f backend/pom.xml -Pnative native:compile -DskipTests -Dfrontend.skip=true

# Stage 3: Minimal runtime container running the native executable
FROM debian:bookworm-slim
WORKDIR /app
COPY --from=builder /app/backend/target/admissions app
ENV PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["sh", "-c", "./app --server.port=${PORT}"]


