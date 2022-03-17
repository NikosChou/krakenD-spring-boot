FROM maven:3.8.4-openjdk-17-slim as builder
ADD ./pom.xml pom.xml
ADD ./src src/
RUN mvn clean package

FROM openjdk:17-slim as runner
ARG JAR_FILE=target/*.jar
COPY --from=builder ${JAR_FILE} /opt/spring/app.jar
ENTRYPOINT ["java", "-jar", "/opt/spring/app.jar"]
EXPOSE 8080

