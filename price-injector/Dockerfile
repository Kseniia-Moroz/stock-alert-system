# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /home/app

RUN apk add --no-cache maven

# Copy Parent POM
COPY pom.xml /home/app/pom.xml
RUN mvn install -N -DskipTests

# Copy Common Module
COPY common /home/app/common
RUN cd /home/app/common && mvn clean install -DskipTests

# Build Price Injector
COPY price-injector /home/app/price-injector
WORKDIR /home/app/price-injector
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S runner && adduser -S runner -G runner
USER runner

# Target the assembly JAR
COPY --from=build /home/app/price-injector/target/price-injector-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-Dmicronaut.environments=k8s", "-jar", "app.jar"]