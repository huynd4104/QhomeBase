FROM openjdk:17

EXPOSE 8086

ADD target/customer-interaction-service-*.jar customer-interaction-service.jar

ENTRYPOINT ["java", "-jar", "/customer-interaction-service.jar"]