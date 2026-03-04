# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /home/app

# Install Maven
RUN apk add --no-cache maven

# Copy Parent POM and install it
COPY pom.xml /home/app/pom.xml
RUN mvn install -N -DskipTests

# Copy and install Common module
COPY common /home/app/common
RUN cd /home/app/common && mvn install -DskipTests

# Copy and build Alert Engine
COPY alert-engine /home/app/alert-engine
WORKDIR /home/app/alert-engine
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S runner && adduser -S runner -G runner
USER runner

# Target the Fat JAR created by the assembly plugin
COPY --from=build /home/app/alert-engine/target/alert-engine-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Dmicronaut.environments=k8s", "-jar", "app.jar"]