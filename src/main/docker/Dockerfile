FROM openjdk:8-jdk-alpine
LABEL maintainer="keyridan@gmail.com"
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
COPY spec.yaml /spec.yaml
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]