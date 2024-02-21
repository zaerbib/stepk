FROM java:17-jdk-alpine
LABEL authors="joel"
COPY ./target/ingestion-service-1.0.0-SNAPSHOT-all.jar /usr/app
WORKDIR /usr/app
EXPOSE 3002

ENTRYPOINT ["java", "-jar", "ingestion-service-1.0.0-SNAPSHOT-all.jar"]
