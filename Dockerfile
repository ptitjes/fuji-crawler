FROM openjdk:11-jre-slim

ENV APPLICATION_USER fuji
RUN adduser $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/fuji-crawler-1.0-SNAPSHOT-all.jar /app/application.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-server", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "application.jar"]
