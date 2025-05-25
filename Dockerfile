## Build stage ##
FROM maven:3.9.6-eclipse-temurin-21 as build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests=true

## Run stage ##
FROM eclipse-temurin:21-jre

RUN adduser --disabled-password --home /run bank

WORKDIR /run
COPY --from=build /app/target/OnlineBankingApp-0.0.1-SNAPSHOT.war /run/OnlineBankingApp-0.0.1-SNAPSHOT.war

RUN chown -R bank:bank /run

USER bank

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/run/OnlineBankingApp-0.0.1-SNAPSHOT.war"]
