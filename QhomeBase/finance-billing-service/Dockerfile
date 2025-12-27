FROM openjdk:17

EXPOSE 8085

ADD target/finance-billing-service-*.jar finance-billing-service.jar

ENTRYPOINT ["java", "-jar", "/finance-billing-service.jar"]