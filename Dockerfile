FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar /app/hc.jar

ENV PORT=3012

EXPOSE 3012

ENTRYPOINT ["java", "-jar", "hc.jar"]
