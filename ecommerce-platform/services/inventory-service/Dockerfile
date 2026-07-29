FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/inventory-service-exec.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
