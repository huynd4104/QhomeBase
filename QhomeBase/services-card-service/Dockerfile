FROM openjdk:17

EXPOSE 8083

ADD target/services-card-service-*.jar services-card-service.jar

ENTRYPOINT ["java", "-jar", "/services-card-service.jar"]