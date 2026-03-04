# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /home/app

# Install Maven (optional if you prefer using the mvnw wrapper)
RUN apk add --no-cache maven

# 1. Copy and Install the PARENT POM first
# This is the crucial step you were missing!
COPY pom.xml /home/app/pom.xml
RUN mvn install -N -DskipTests

# 2. Copy and Install the COMMON module
COPY common /home/app/common
RUN cd /home/app/common && mvn clean install -DskipTests

# 3. Copy and build the MARKET-DATA service
COPY market-data-service /home/app/market-data-service
WORKDIR /home/app/market-data-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S runner && adduser -S runner -G runner
USER runner

COPY --from=build /home/app/market-data-service/target/market-data-service-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dmicronaut.environments=k8s", "-jar", "app.jar"]