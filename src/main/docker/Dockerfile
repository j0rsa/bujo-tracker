FROM openjdk:8-jdk-alpine
LABEL maintainer="keyridan@gmail.com"
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]