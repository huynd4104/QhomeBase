FROM openjdk:17

EXPOSE 8084

ADD target/asset-maintenance-service-*.jar asset-maintenance-service.jar

ENTRYPOINT ["java", "-jar", "/asset-maintenance-service.jar"]