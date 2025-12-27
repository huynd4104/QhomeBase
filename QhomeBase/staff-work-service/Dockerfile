FROM openjdk:17

EXPOSE 8087

ADD target/staff-work-service-*.jar staff-work-service.jar

ENTRYPOINT ["java", "-jar", "/staff-work-service.jar"]