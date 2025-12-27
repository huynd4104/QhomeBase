FROM openjdk:17

EXPOSE 8082

ADD target/data-docs-service-*.jar data-docs-service.jar

ENTRYPOINT ["java", "-jar", "/data-docs-service.jar"]