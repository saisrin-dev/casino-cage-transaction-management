FROM node:22-alpine AS ui
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
FROM maven:3.9.11-eclipse-temurin-17 AS java
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=ui /build/frontend/dist/cage/browser/ ./src/main/resources/static/
RUN mvn -B package -DskipTests
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S cage && adduser -S cage -G cage
COPY --from=java --chown=cage:cage /build/backend/target/casino-cage-1.0.0.jar /app/app.jar
USER cage
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC"
ENTRYPOINT ["java","-jar","/app/app.jar"]
