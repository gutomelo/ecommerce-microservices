FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/product-service-exec.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
