FROM adoptopenjdk/openjdk12:x86_64-alpine-jdk-12.0.2_10-slim

COPY target/mext.jar /mext/app.jar

CMD ["java", "-jar", "/mext/app.jar"]
