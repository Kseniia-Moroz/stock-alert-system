
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /home/app

RUN apk add --no-cache maven

COPY pom.xml /home/app/pom.xml
RUN mvn install -N -DskipTests

COPY common /home/app/common
RUN cd /home/app/common && mvn clean install -DskipTests

COPY market-data-service /home/app/market-data-service
WORKDIR /home/app/market-data-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S runner && adduser -S runner -G runner
USER runner

COPY --from=build /home/app/market-data-service/target/market-data-service-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-Dmicronaut.environments=k8s", "-jar", "app.jar"]