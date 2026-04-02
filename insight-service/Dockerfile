FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib ./common-lib
COPY insight-service/pom.xml ./insight-service/
COPY insight-service/src ./insight-service/src
RUN mvn clean install -N -DskipTests
RUN mvn -f common-lib/pom.xml clean install -DskipTests
RUN mvn -f insight-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/insight-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
