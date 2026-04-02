FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib ./common-lib
COPY transaction-service/pom.xml ./transaction-service/
COPY transaction-service/src ./transaction-service/src
RUN mvn clean install -N -DskipTests
RUN mvn -f common-lib/pom.xml clean install -DskipTests
RUN mvn -f transaction-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/transaction-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
