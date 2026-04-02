FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib ./common-lib
COPY user-service/pom.xml ./user-service/
COPY user-service/src ./user-service/src
RUN mvn clean install -N -DskipTests
RUN mvn -f common-lib/pom.xml clean install -DskipTests
RUN mvn -f user-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
