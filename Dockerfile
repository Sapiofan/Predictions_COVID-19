FROM openjdk:11
ADD target/predictions.jar predictions.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "predictions.jar"]