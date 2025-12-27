FROM openjdk:17

EXPOSE 8081

ADD target/base-service-*.jar base-service.jar

ENTRYPOINT ["java", "-jar", "/base-service.jar"]