FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/payment-service-exec.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
