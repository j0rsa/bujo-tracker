FROM openjdk:8-jdk-alpine
LABEL maintainer="keyridan@gmail.com"
ADD ${JAR_FILE} app.jar
CMD ["java","-jar","/app.jar"]