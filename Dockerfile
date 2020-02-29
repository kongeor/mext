FROM openjdk:8-alpine

COPY target/mext.jar /mext/app.jar

CMD ["java", "-jar", "/mext/app.jar"]
